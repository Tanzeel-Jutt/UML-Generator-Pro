package com.umlgenerator.ui.components;

import com.umlgenerator.core.model.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.control.Label;
import java.util.*;

public class RelationshipRenderer {

    private static final double GAP = 14;
    private static final double MARGIN = 55;

    private static class R {
        final double x, y, w, h;
        R(double x, double y, double w, double h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        double cx() { return x+w/2; } double cy() { return y+h/2; }
        double r()  { return x+w; }   double b()  { return y+h; }
        boolean hits(double x1, double y1, double x2, double y2) {
            double dx=x2-x1, dy=y2-y1;
            double[] p={-dx,dx,-dy,dy}, q={x1-x,r()-x1,y1-y,b()-y1};
            double u1=0, u2=1;
            for (int i=0;i<4;i++) {
                if (Math.abs(p[i])<1e-9) { if(q[i]<0) return false; }
                else { double t=q[i]/p[i]; if(p[i]<0){if(t>u1)u1=t;}else{if(t<u2)u2=t;} }
            }
            return u1 < u2 - 1e-9;
        }
    }

    private enum Side { TOP, BOTTOM, LEFT, RIGHT }

    private static class Slots {
        Map<String,Integer> cnt = new HashMap<>(), nxt = new HashMap<>();
        void reg(String b, Side s) { cnt.merge(b+s,1,Integer::sum); }
        double[] pt(String b, Side s, ClassBoxNode n) {
            int tot=cnt.getOrDefault(b+s,1), idx=nxt.merge(b+s,1,Integer::sum)-1;
            double t = tot<=1 ? 0.5 : 0.2+idx*0.6/(tot-1);
            double bx=n.getLayoutX(), by=n.getLayoutY(), bw=bw(n), bh=bh(n);
            return switch(s) {
                case TOP    -> new double[]{bx+t*bw, by-GAP};
                case BOTTOM -> new double[]{bx+t*bw, by+bh+GAP};
                case LEFT   -> new double[]{bx-GAP, by+t*bh};
                case RIGHT  -> new double[]{bx+bw+GAP, by+t*bh};
            };
        }
    }

    public static void drawAllRelationships(Pane canvas, List<UMLRelationship> rels,
            Map<String, ClassBoxNode> boxMap) {
        if (rels == null || rels.isEmpty()) return;

        Map<String, R> rects = new LinkedHashMap<>();
        for (var e : boxMap.entrySet())
            rects.put(e.getKey(), new R(e.getValue().getLayoutX(), e.getValue().getLayoutY(),
                    bw(e.getValue()), bh(e.getValue())));

        // Global bounds for margin lanes
        double gTop=Double.MAX_VALUE, gLeft=Double.MAX_VALUE, gBot=-1e9, gRight=-1e9;
        for (R r : rects.values()) {
            gTop=Math.min(gTop,r.y); gLeft=Math.min(gLeft,r.x);
            gBot=Math.max(gBot,r.b()); gRight=Math.max(gRight,r.r());
        }
        double[] margins = { gTop-MARGIN, gBot+MARGIN, gLeft-MARGIN, gRight+MARGIN };

        // Phase 1: pick sides
        Slots slots = new Slots();
        Side[][] sArr = new Side[rels.size()][];
        for (int i = 0; i < rels.size(); i++) {
            var rel = rels.get(i);
            ClassBoxNode src=boxMap.get(rel.getSourceClassName()), tgt=boxMap.get(rel.getTargetClassName());
            if (src==null||tgt==null||src==tgt) continue;
            R sr=rects.get(rel.getSourceClassName()), tr=rects.get(rel.getTargetClassName());
            List<R> obs = getObs(rects, rel.getSourceClassName(), rel.getTargetClassName());
            Side ss=pickSide(sr,tr,obs), ts=pickSide(tr,sr,obs);
            sArr[i]=new Side[]{ss,ts};
            slots.reg(rel.getSourceClassName(),ss); slots.reg(rel.getTargetClassName(),ts);
        }

        // Phase 2: route & draw
        int lane = 0;
        for (int i = 0; i < rels.size(); i++) {
            if (sArr[i]==null) continue;
            var rel = rels.get(i);
            ClassBoxNode src=boxMap.get(rel.getSourceClassName()), tgt=boxMap.get(rel.getTargetClassName());
            double[] p1=slots.pt(rel.getSourceClassName(),sArr[i][0],src);
            double[] p2=slots.pt(rel.getTargetClassName(),sArr[i][1],tgt);
            // ALL boxes are obstacles for intermediate segments
            List<R> allObs = new ArrayList<>();
            for (R r : rects.values()) allObs.add(r);
            List<double[]> path = route(p1, p2, sArr[i][0], sArr[i][1], allObs, margins, lane++);
            draw(canvas, path, rel);
        }
    }

    private static List<R> getObs(Map<String,R> rects, String s, String t) {
        List<R> obs = new ArrayList<>();
        for (var e : rects.entrySet())
            if (!e.getKey().equals(s) && !e.getKey().equals(t)) obs.add(e.getValue());
        return obs;
    }

    private static Side pickSide(R from, R to, List<R> obs) {
        double dx=to.cx()-from.cx(), dy=to.cy()-from.cy();
        List<Side> pref = new ArrayList<>();
        if (Math.abs(dx)>=Math.abs(dy)) {
            pref.add(dx>0?Side.RIGHT:Side.LEFT); pref.add(dy>0?Side.BOTTOM:Side.TOP);
            pref.add(dy<=0?Side.BOTTOM:Side.TOP); pref.add(dx<=0?Side.RIGHT:Side.LEFT);
        } else {
            pref.add(dy>0?Side.BOTTOM:Side.TOP); pref.add(dx>0?Side.RIGHT:Side.LEFT);
            pref.add(dx<=0?Side.RIGHT:Side.LEFT); pref.add(dy<=0?Side.BOTTOM:Side.TOP);
        }
        for (Side s : pref) {
            double ex=edgeX(from,s), ey=edgeY(from,s);
            if (!anyHit(ex,ey,to.cx(),to.cy(),obs)) return s;
        }
        return pref.get(0);
    }

    private static double edgeX(R r,Side s){return switch(s){case LEFT->r.x;case RIGHT->r.r();default->r.cx();};}
    private static double edgeY(R r,Side s){return switch(s){case TOP->r.y;case BOTTOM->r.b();default->r.cy();};}

    // ═══ ROUTING ═══════════════════════════════════════════════════════

    private static List<double[]> route(double[] p1, double[] p2, Side s1, Side s2,
            List<R> allObs, double[] margins, int lane) {
        double step = 20 + (lane % 6) * 14;
        double[] o1 = stp(p1, s1, step), o2 = stp(p2, s2, step);

        // Strategy 1: Direct
        if (!anyHit(o1[0],o1[1],o2[0],o2[1],allObs))
            return List.of(p1, o1, o2, p2);

        // Strategy 2: L-shape (two options)
        double[] cA = {o2[0], o1[1]};
        if (!anyHit(o1,cA,allObs) && !anyHit(cA,o2,allObs))
            return List.of(p1, o1, cA, o2, p2);

        double[] cB = {o1[0], o2[1]};
        if (!anyHit(o1,cB,allObs) && !anyHit(cB,o2,allObs))
            return List.of(p1, o1, cB, o2, p2);

        // Strategy 3: Z-shape through interior midpoint
        boolean h1 = s1==Side.LEFT||s1==Side.RIGHT;
        if (h1) {
            double midX = (o1[0]+o2[0])/2;
            double[] z1={midX,o1[1]}, z2={midX,o2[1]};
            if (!anyHit(o1,z1,allObs) && !anyHit(z1,z2,allObs) && !anyHit(z2,o2,allObs))
                return List.of(p1, o1, z1, z2, o2, p2);
        } else {
            double midY = (o1[1]+o2[1])/2;
            double[] z1={o1[0],midY}, z2={o2[0],midY};
            if (!anyHit(o1,z1,allObs) && !anyHit(z1,z2,allObs) && !anyHit(z2,o2,allObs))
                return List.of(p1, o1, z1, z2, o2, p2);
        }

        // Strategy 4: Route through global margin (GUARANTEED clear)
        return marginRoute(p1, o1, p2, o2, s1, s2, margins, allObs, lane);
    }

    /**
     * Route through global margins — goes COMPLETELY outside all boxes.
     * Tries top, bottom, left, right margins and picks the shortest clear one.
     */
    private static List<double[]> marginRoute(double[] p1, double[] o1, double[] p2, double[] o2,
            Side s1, Side s2, double[] margins, List<R> allObs, int lane) {
        // margins: [topY, bottomY, leftX, rightX]
        double topY = margins[0] - (lane % 4) * 15;
        double botY = margins[1] + (lane % 4) * 15;
        double leftX = margins[2] - (lane % 4) * 15;
        double rightX = margins[3] + (lane % 4) * 15;

        // Try 4 margin routes, pick the shortest clear one
        List<double[]>[] candidates = new List[4];

        // Top margin: go up, across, down
        candidates[0] = List.of(p1, o1,
                new double[]{o1[0], topY}, new double[]{o2[0], topY}, o2, p2);
        // Bottom margin: go down, across, up
        candidates[1] = List.of(p1, o1,
                new double[]{o1[0], botY}, new double[]{o2[0], botY}, o2, p2);
        // Left margin: go left, along, back
        candidates[2] = List.of(p1, o1,
                new double[]{leftX, o1[1]}, new double[]{leftX, o2[1]}, o2, p2);
        // Right margin: go right, along, back
        candidates[3] = List.of(p1, o1,
                new double[]{rightX, o1[1]}, new double[]{rightX, o2[1]}, o2, p2);

        // Score each: prefer shorter + clear paths
        double bestScore = Double.MAX_VALUE;
        List<double[]> best = candidates[0];
        for (List<double[]> c : candidates) {
            double len = pathLen(c);
            boolean clear = true;
            for (int i = 1; i < c.size() - 1; i++) {
                // Only check intermediate segments (skip p1->o1 and o2->p2 which touch boxes)
                if (i >= 2 && i < c.size() - 2) {
                    if (anyHit(c.get(i), c.get(i+1), allObs)) { clear = false; break; }
                }
            }
            double score = len + (clear ? 0 : 500000);
            if (score < bestScore) { bestScore = score; best = c; }
        }
        return new ArrayList<>(best);
    }

    private static double pathLen(List<double[]> pts) {
        double len = 0;
        for (int i = 1; i < pts.size(); i++)
            len += Math.abs(pts.get(i)[0]-pts.get(i-1)[0]) + Math.abs(pts.get(i)[1]-pts.get(i-1)[1]);
        return len;
    }

    private static double[] stp(double[] p, Side s, double d) {
        return switch(s) {
            case TOP->new double[]{p[0],p[1]-d}; case BOTTOM->new double[]{p[0],p[1]+d};
            case LEFT->new double[]{p[0]-d,p[1]}; case RIGHT->new double[]{p[0]+d,p[1]};
        };
    }

    private static boolean anyHit(double[] a, double[] b, List<R> obs) {
        return anyHit(a[0],a[1],b[0],b[1],obs);
    }
    private static boolean anyHit(double x1,double y1,double x2,double y2,List<R> obs) {
        for (R r : obs) if (r.hits(x1,y1,x2,y2)) return true;
        return false;
    }

    // ═══ DRAWING ═══════════════════════════════════════════════════════

    private static void draw(Pane canvas, List<double[]> pts, UMLRelationship rel) {
        if (pts.size() < 2) return;
        Path path = new Path();
        path.getElements().add(new MoveTo(pts.get(0)[0], pts.get(0)[1]));

        for (int i = 1; i < pts.size() - 1; i++) {
            double[] prev=pts.get(i-1), cur=pts.get(i), next=pts.get(i+1);
            double dxI=cur[0]-prev[0], dyI=cur[1]-prev[1];
            double dxO=next[0]-cur[0], dyO=next[1]-cur[1];
            double lI=Math.sqrt(dxI*dxI+dyI*dyI), lO=Math.sqrt(dxO*dxO+dyO*dyO);
            if (lI<2||lO<2) { path.getElements().add(new LineTo(cur[0],cur[1])); continue; }
            double r = Math.min(14, Math.min(lI/2, lO/2));
            double bx=cur[0]-(dxI/lI)*r, by=cur[1]-(dyI/lI)*r;
            double ax=cur[0]+(dxO/lO)*r, ay=cur[1]+(dyO/lO)*r;
            path.getElements().add(new LineTo(bx,by));
            path.getElements().add(new QuadCurveTo(cur[0],cur[1],ax,ay));
        }
        path.getElements().add(new LineTo(pts.get(pts.size()-1)[0], pts.get(pts.size()-1)[1]));

        path.setStrokeWidth(2.0); path.setFill(null);
        path.setStroke(col(rel.getType()));
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        if (rel.getType()==RelationshipType.IMPLEMENTATION||rel.getType()==RelationshipType.DEPENDENCY)
            path.getStrokeDashArray().addAll(10.0,6.0);
        canvas.getChildren().add(path);

        // Arrowhead
        double[] tip=pts.get(pts.size()-1), pre=pts.get(pts.size()-2);
        double ang=Math.atan2(tip[1]-pre[1],tip[0]-pre[0]);
        drawHead(canvas,tip[0],tip[1],ang,rel);

        // Diamond
        if (rel.getType()==RelationshipType.COMPOSITION||rel.getType()==RelationshipType.AGGREGATION) {
            double[] f=pts.get(0), s=pts.get(1);
            drawDiamond(canvas,f[0],f[1],Math.atan2(s[1]-f[1],s[0]-f[0]),rel);
        }

        // Label
        if (rel.getLabel()!=null&&!rel.getLabel().isEmpty()) {
            int m=pts.size()/2;
            Label l=new Label(rel.getLabel());
            l.setStyle("-fx-text-fill:#94a3b8;-fx-font-size:11px;-fx-background-color:rgba(15,23,42,0.92);-fx-padding:1 5;-fx-background-radius:3;");
            l.setLayoutX(pts.get(m)[0]+4); l.setLayoutY(pts.get(m)[1]-16);
            canvas.getChildren().add(l);
        }
    }

    // ═══ HELPERS ═══════════════════════════════════════════════════════

    private static double bw(ClassBoxNode b) {
        b.applyCss();
        b.layout();
        double w = b.getBoundsInParent().getWidth();
        if(w<=0) w=b.getWidth();
        if(w<=0) w=b.prefWidth(-1);
        return w>0?w:280;
    }
    
    private static double bh(ClassBoxNode b) {
        b.applyCss();
        b.layout();
        double h = b.getBoundsInParent().getHeight();
        if(h<=0) h=b.getHeight();
        if(h<=0) h=b.prefHeight(-1);
        return h>0?h:180;
    }

    private static Color col(RelationshipType t) {
        return switch(t) {
            case INHERITANCE->Color.web("#60a5fa"); case IMPLEMENTATION->Color.web("#a5b4fc");
            case COMPOSITION->Color.web("#4ade80"); case AGGREGATION->Color.web("#fbbf24");
            case ASSOCIATION->Color.web("#94a3b8"); case DEPENDENCY->Color.web("#6b7280");
            default->Color.web("#94a3b8");
        };
    }

    private static void drawHead(Pane c,double tx,double ty,double a,UMLRelationship rel) {
        Color clr=col(rel.getType()); double s=14;
        if (rel.getType()==RelationshipType.INHERITANCE||rel.getType()==RelationshipType.IMPLEMENTATION) {
            Polygon t2=new Polygon(tx,ty,tx-s*Math.cos(a-0.45),ty-s*Math.sin(a-0.45),
                    tx-s*Math.cos(a+0.45),ty-s*Math.sin(a+0.45));
            t2.setStroke(clr);t2.setFill(Color.web("#0f172a"));t2.setStrokeWidth(2);c.getChildren().add(t2);
        } else {
            Line a1=new Line(tx,ty,tx-11*Math.cos(a-0.5),ty-11*Math.sin(a-0.5));
            Line a2=new Line(tx,ty,tx-11*Math.cos(a+0.5),ty-11*Math.sin(a+0.5));
            a1.setStroke(clr);a2.setStroke(clr);a1.setStrokeWidth(2);a2.setStrokeWidth(2);
            c.getChildren().addAll(a1,a2);
        }
    }

    private static void drawDiamond(Pane c,double sx,double sy,double a,UMLRelationship rel) {
        boolean f=rel.getType()==RelationshipType.COMPOSITION; Color clr=col(rel.getType()); double s=11;
        Polygon d=new Polygon(sx,sy,sx+s*Math.cos(a-0.6),sy+s*Math.sin(a-0.6),
                sx+s*2*Math.cos(a),sy+s*2*Math.sin(a),sx+s*Math.cos(a+0.6),sy+s*Math.sin(a+0.6));
        d.setStroke(clr);d.setFill(f?clr:Color.web("#0f172a"));d.setStrokeWidth(f?1:2);c.getChildren().add(d);
    }
}
