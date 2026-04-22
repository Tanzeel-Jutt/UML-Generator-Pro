// Renderers for UML Generator Pro Web
// Implements PERFECT Desktop (JavaFX) Logic using HTML DOM for crisp text & exact AST Routing

const renderers = {
  uml: {
    render(containerId, diagram) {
      if (!diagram) return;
      const panel = document.getElementById('panel-uml');
      
      panel.innerHTML = `
         <div id="uml-zoom-controls" style="position:absolute; bottom:20px; right:20px; z-index:10; background:var(--surface); border:1px solid var(--border); border-radius:6px; display:flex; gap:1px; box-shadow:var(--shadow);">
             <button id="btn-zoom-out" style="padding:6px 12px; background:none; border:none; color:white; cursor:pointer; font-size:16px;">➖</button>
             <button id="btn-zoom-reset" style="padding:6px 12px; background:none; border:none; color:var(--text2); cursor:pointer; font-size:12px; font-weight:bold; min-width:60px;">100%</button>
             <button id="btn-zoom-in" style="padding:6px 12px; background:none; border:none; color:white; cursor:pointer; font-size:16px;">➕</button>
         </div>
         <div id="uml-scroll-area" style="width:100%; height:100%; overflow:auto; position:relative; background:var(--bg);">
            <div id="uml-content-area" style="position:relative; width:4000px; height:4000px; transform-origin: top left; transition: transform 0.15s ease-out;">
               <svg id="uml-svg" style="position:absolute; top:0; left:0; width:100%; height:100%; pointer-events:none; z-index:1; overflow:visible;"></svg>
               <div id="uml-boxes" style="position:absolute; top:0; left:0; width:100%; height:100%; z-index:2;"></div>
            </div>
         </div>
      `;
      
      const boxesContainer = document.getElementById('uml-boxes');
      const svgContainer = document.getElementById('uml-svg');
      
      // 1. Desktop Logic: UMLClassDiagramGenerator (Grid Layout)
      let currentX = 100;
      let currentY = 100;
      let rowMaxHeight = 0;
      const boxElements = {};
      
      diagram.classes.forEach(cls => {
         const box = document.createElement('div');
         box.className = 'class-box-node';
         box.style.position = 'absolute';
         box.style.left = currentX + 'px';
         box.style.top = currentY + 'px';
         box.style.width = '280px';
         box.style.backgroundColor = '#1e1e2e';
         box.style.borderRadius = '8px';
         box.style.boxShadow = '0 3px 10px rgba(0,0,0,0.3)';
         box.style.display = 'flex';
         box.style.flexDirection = 'column';
         box.style.fontFamily = 'var(--font)';
         
         let headerBg = '#1a1a2e';
         if (cls.type === 'ABSTRACT_CLASS') headerBg = '#16213e';
         if (cls.type === 'INTERFACE') headerBg = '#0f3460';
         if (cls.type === 'ENUM') headerBg = '#533483';
         if (cls.type === 'RECORD') headerBg = '#2b2d42';
         
         let headerHTML = `<div style="background-color:${headerBg}; border-radius:8px 8px 0 0; padding:8px 12px; text-align:center;">`;
         let stType = cls.type.toLowerCase();
         if (stType === 'abstract_class') stType = 'abstract';
         if (cls.name.toLowerCase().includes('exception')) stType = 'exception';
         
         if (stType !== 'class') {
             headerHTML += `<div style="color:#a5b4fc; font-size:10px; font-style:italic;">&lt;&lt;${stType}&gt;&gt;</div>`;
         }
         let nameStyle = 'color:white; font-size:14px; font-weight:bold;';
         if (cls.type === 'ABSTRACT_CLASS') nameStyle += ' font-style:italic;';
         headerHTML += `<div style="${nameStyle}">${cls.name}</div></div>`;
         headerHTML += `<div style="height:1px; background-color:#334155;"></div>`;
         
         let attrHTML = `<div style="padding:6px 10px; font-family:'Consolas', monospace; font-size:11px;">`;
         if (cls.constants && cls.constants.length > 0) {
             attrHTML += `<div style="color:#a5b4fc; font-size:9px; font-style:italic;">&lt;&lt;values&gt;&gt;</div>`;
             cls.constants.forEach(c => { attrHTML += `<div style="color:#fbbf24;">&nbsp;&nbsp;${c}</div>`; });
             attrHTML += `<div style="height:1px; background-color:#334155; margin:4px 0;"></div>`;
         }
         if (!cls.attributes || cls.attributes.length === 0) {
             attrHTML += `<div style="color:#64748b; font-style:italic; font-size:10px;">&nbsp;&nbsp;(no attributes)</div>`;
         } else {
             cls.attributes.forEach(attr => {
                 let style = `color:${getAccColor(attr.access)}; padding:1px 0;`;
                 if (attr.isStatic) style += ' text-decoration:underline;';
                 let fStr = attr.isFinal ? ' {final}' : '';
                 attrHTML += `<div style="${style}">${getAccChar(attr.access)} ${attr.name}: ${attr.type}${fStr}</div>`;
             });
         }
         attrHTML += `</div>`;
         
         let methHTML = `<div style="height:1px; background-color:#334155;"></div>`;
         methHTML += `<div style="padding:6px 10px 8px 10px; font-family:'Consolas', monospace; font-size:11px; border-radius:0 0 8px 8px;">`;
         if (!cls.methods || cls.methods.length === 0) {
             methHTML += `<div style="color:#64748b; font-style:italic; font-size:10px;">&nbsp;&nbsp;(no methods)</div>`;
         } else {
             cls.methods.forEach(meth => {
                 let style = `color:${getAccColor(meth.access)}; padding:1px 0;`;
                 if (meth.isStatic) style += ' text-decoration:underline;';
                 if (meth.isAbstract) style += ' font-style:italic;';
                 if (meth.isConstructor) style += ' font-weight:bold;';
                 const returnStr = meth.isConstructor ? '' : `: ${meth.returnType}`;
                 let fStr = meth.isFinal ? ' {final}' : '';
                 methHTML += `<div style="${style}">${getAccChar(meth.access)} ${meth.name}(${meth.params || ''})${returnStr}${fStr}</div>`;
             });
         }
         methHTML += `</div>`;
         
         box.innerHTML = headerHTML + attrHTML + methHTML;
         boxesContainer.appendChild(box);
         boxElements[cls.name] = box;
         
         setTimeout(() => { rowMaxHeight = Math.max(rowMaxHeight, box.offsetHeight); }, 0);
         
         currentX += 450;
         if (currentX > 3500) { currentX = 100; currentY += 500; }
      });
      
      // 2. Desktop Logic: EXACT RelationshipRenderer algorithm
      setTimeout(() => {
          currentX = 100; currentY = 100; rowMaxHeight = 0;
          Object.values(boxElements).forEach(box => {
              box.style.left = currentX + 'px';
              box.style.top = currentY + 'px';
              rowMaxHeight = Math.max(rowMaxHeight, box.offsetHeight);
              currentX += 450;
              if (currentX > 3500) { currentX = 100; currentY += rowMaxHeight + 150; rowMaxHeight = 0; }
          });
          
          class R {
              constructor(x, y, w, h) { this.x=x; this.y=y; this.w=w; this.h=h; }
              cx() { return this.x+this.w/2; } cy() { return this.y+this.h/2; }
              r() { return this.x+this.w; } b() { return this.y+this.h; }
              hits(x1, y1, x2, y2) {
                  let dx=x2-x1, dy=y2-y1;
                  let p = [-dx, dx, -dy, dy];
                  let q = [x1-this.x, this.r()-x1, y1-this.y, this.b()-y1];
                  let u1=0, u2=1;
                  for(let i=0; i<4; i++) {
                      if(Math.abs(p[i])<1e-9) { if(q[i]<0) return false; }
                      else { let t = q[i]/p[i]; if(p[i]<0) { if(t>u1) u1=t; } else { if(t<u2) u2=t; } }
                  }
                  return u1 < u2 - 1e-9;
              }
          }

          class Slots {
              constructor() { this.cnt={}; this.nxt={}; }
              reg(b, s) { this.cnt[b+s] = (this.cnt[b+s]||0)+1; }
              pt(b, s, r) {
                  let tot=this.cnt[b+s]||1;
                  this.nxt[b+s] = (this.nxt[b+s]||0)+1;
                  let idx=this.nxt[b+s]-1;
                  let t = tot<=1 ? 0.5 : 0.2+idx*0.6/(tot-1);
                  if(s==='TOP') return [r.x+t*r.w, r.y-14];
                  if(s==='BOTTOM') return [r.x+t*r.w, r.b()+14];
                  if(s==='LEFT') return [r.x-14, r.y+t*r.h];
                  if(s==='RIGHT') return [r.r()+14, r.y+t*r.h];
              }
          }

          const rects = {};
          let gTop=Infinity, gLeft=Infinity, gBot=-Infinity, gRight=-Infinity;
          for(let name in boxElements) {
              const el = boxElements[name];
              const r = new R(el.offsetLeft, el.offsetTop, el.offsetWidth, el.offsetHeight);
              rects[name] = r;
              gTop = Math.min(gTop, r.y); gLeft = Math.min(gLeft, r.x);
              gBot = Math.max(gBot, r.b()); gRight = Math.max(gRight, r.r());
          }
          const margins = [gTop-55, gBot+55, gLeft-55, gRight+55];
          
          function anyHitSegment(x1,y1,x2,y2, obs) { return obs.some(o => o.hits(x1,y1,x2,y2)); }
          function edgeX(r, s) { return s==='LEFT'?r.x : s==='RIGHT'?r.r() : r.cx(); }
          function edgeY(r, s) { return s==='TOP'?r.y : s==='BOTTOM'?r.b() : r.cy(); }
          function pickSide(from, to, obs) {
              let dx=to.cx()-from.cx(), dy=to.cy()-from.cy();
              let pref = [];
              if(Math.abs(dx)>=Math.abs(dy)) { pref.push(dx>0?'RIGHT':'LEFT', dy>0?'BOTTOM':'TOP', dy<=0?'BOTTOM':'TOP', dx<=0?'RIGHT':'LEFT'); }
              else { pref.push(dy>0?'BOTTOM':'TOP', dx>0?'RIGHT':'LEFT', dx<=0?'RIGHT':'LEFT', dy<=0?'BOTTOM':'TOP'); }
              for(let s of pref) { if(!anyHitSegment(edgeX(from,s), edgeY(from,s), to.cx(), to.cy(), obs)) return s; }
              return pref[0];
          }

          const slots = new Slots();
          const sArr = [];
          diagram.relationships.forEach(rel => {
              const sr = rects[rel.source], tr = rects[rel.target];
              if(!sr || !tr) { sArr.push(null); return; }
              const obs = Object.entries(rects).filter(([k,v]) => k!==rel.source && k!==rel.target).map(e=>e[1]);
              const ss = pickSide(sr, tr, obs), ts = pickSide(tr, sr, obs);
              sArr.push([ss, ts]);
              slots.reg(rel.source, ss); slots.reg(rel.target, ts);
          });

          let lane = 0;
          let svgContent = '';
          const allObs = Object.values(rects);
          
          diagram.relationships.forEach((rel, i) => {
              if(!sArr[i]) return;
              const [ss, ts] = sArr[i];
              const p1 = slots.pt(rel.source, ss, rects[rel.source]);
              const p2 = slots.pt(rel.target, ts, rects[rel.target]);
              
              const step = 20 + (lane%6)*14;
              const o1 = stp(p1, ss, step), o2 = stp(p2, ts, step);
              
              let pathPts = [];
              if(!anyHitSegment(o1[0],o1[1],o2[0],o2[1],allObs)) {
                  pathPts = [p1, o1, o2, p2];
              } else {
                  let cA = [o2[0], o1[1]], cB = [o1[0], o2[1]];
                  if(!anyHitSegment(o1[0],o1[1],cA[0],cA[1],allObs) && !anyHitSegment(cA[0],cA[1],o2[0],o2[1],allObs)) { pathPts = [p1, o1, cA, o2, p2]; }
                  else if(!anyHitSegment(o1[0],o1[1],cB[0],cB[1],allObs) && !anyHitSegment(cB[0],cB[1],o2[0],o2[1],allObs)) { pathPts = [p1, o1, cB, o2, p2]; }
                  else {
                      let topY = margins[0] - (lane%4)*15, botY = margins[1] + (lane%4)*15;
                      let leftX = margins[2] - (lane%4)*15, rightX = margins[3] + (lane%4)*15;
                      let cands = [
                          [p1, o1, [o1[0], topY], [o2[0], topY], o2, p2],
                          [p1, o1, [o1[0], botY], [o2[0], botY], o2, p2],
                          [p1, o1, [leftX, o1[1]], [leftX, o2[1]], o2, p2],
                          [p1, o1, [rightX, o1[1]], [rightX, o2[1]], o2, p2]
                      ];
                      let bestScore=Infinity;
                      for(let c of cands) {
                          let len=0; for(let j=1;j<c.length;j++) len+=Math.abs(c[j][0]-c[j-1][0])+Math.abs(c[j][1]-c[j-1][1]);
                          let clear=true;
                          for(let j=2; j<c.length-2; j++) { if(anyHitSegment(c[j][0],c[j][1],c[j+1][0],c[j+1][1], allObs)) { clear=false; break; } }
                          let score = len + (clear?0:500000);
                          if(score<bestScore) { bestScore=score; pathPts=c; }
                      }
                  }
              }
              
              function stp(p, s, d) {
                  if(s==='TOP') return [p[0], p[1]-d]; if(s==='BOTTOM') return [p[0], p[1]+d];
                  if(s==='LEFT') return [p[0]-d, p[1]]; return [p[0]+d, p[1]];
              }

              const color = getRelColor(rel.type);
              const isDashed = rel.type === 'IMPLEMENTATION' || rel.type === 'DEPENDENCY';
              const dash = isDashed ? 'stroke-dasharray="10,6"' : '';
              
              // Draw rounded path
              let d = `M ${pathPts[0][0]} ${pathPts[0][1]}`;
              for(let j=1; j<pathPts.length-1; j++) {
                  let pr=pathPts[j-1], cu=pathPts[j], nx=pathPts[j+1];
                  let dxI=cu[0]-pr[0], dyI=cu[1]-pr[1], dxO=nx[0]-cu[0], dyO=nx[1]-cu[1];
                  let lI=Math.hypot(dxI,dyI), lO=Math.hypot(dxO,dyO);
                  if(lI<2||lO<2) { d+=` L ${cu[0]} ${cu[1]}`; continue; }
                  let r = Math.min(14, lI/2, lO/2);
                  let bx=cu[0]-(dxI/lI)*r, by=cu[1]-(dyI/lI)*r, ax=cu[0]+(dxO/lO)*r, ay=cu[1]+(dyO/lO)*r;
                  d += ` L ${bx} ${by} Q ${cu[0]} ${cu[1]} ${ax} ${ay}`;
              }
              d += ` L ${pathPts[pathPts.length-1][0]} ${pathPts[pathPts.length-1][1]}`;
              
              svgContent += `<path d="${d}" fill="none" stroke="${color}" stroke-width="2" ${dash} stroke-linecap="round" stroke-linejoin="round" />`;
              
              let tip = pathPts[pathPts.length-1], pre = pathPts[pathPts.length-2];
              let ang = Math.atan2(tip[1]-pre[1], tip[0]-pre[0]);
              
              if (rel.type === 'INHERITANCE' || rel.type === 'IMPLEMENTATION') {
                   let arrowFill = rel.type === 'IMPLEMENTATION' ? 'none' : '#0f172a';
                   svgContent += `<polygon points="${tip[0]},${tip[1]} ${tip[0]-14*Math.cos(ang-0.45)},${tip[1]-14*Math.sin(ang-0.45)} ${tip[0]-14*Math.cos(ang+0.45)},${tip[1]-14*Math.sin(ang+0.45)}" fill="${arrowFill}" stroke="${color}" stroke-width="2" />`;
              } else if (rel.type !== 'ASSOCIATION') {
                  svgContent += `<polyline points="${tip[0]-11*Math.cos(ang-0.5)},${tip[1]-11*Math.sin(ang-0.5)} ${tip[0]},${tip[1]} ${tip[0]-11*Math.cos(ang+0.5)},${tip[1]-11*Math.sin(ang+0.5)}" fill="none" stroke="${color}" stroke-width="2" />`;
              }
              
              if (rel.type === 'COMPOSITION' || rel.type === 'AGGREGATION') {
                  let f = pathPts[0], s = pathPts[1], sa = Math.atan2(s[1]-f[1], s[0]-f[0]);
                  let fill = rel.type === 'COMPOSITION' ? color : '#0f172a';
                  svgContent += `<polygon points="${f[0]},${f[1]} ${f[0]+11*Math.cos(sa-0.6)},${f[1]+11*Math.sin(sa-0.6)} ${f[0]+22*Math.cos(sa)},${f[1]+22*Math.sin(sa)} ${f[0]+11*Math.cos(sa+0.6)},${f[1]+11*Math.sin(sa+0.6)}" fill="${fill}" stroke="${color}" stroke-width="${rel.type==='COMPOSITION'?1:2}" />`;
              }
              lane++;
          });
          
          svgContainer.innerHTML = svgContent;
      }, 50);
      
      function getAccColor(acc) {
         if (acc === 'public') return '#4ade80'; if (acc === 'private') return '#f87171';
         if (acc === 'protected') return '#fbbf24'; return '#60a5fa';
      }
      function getAccChar(acc) {
         if (acc === 'public') return '+'; if (acc === 'private') return '-';
         if (acc === 'protected') return '#'; return '-';
      }
      function getRelColor(type) {
         if (type === 'INHERITANCE') return '#3b82f6';
         if (type === 'IMPLEMENTATION') return '#22d3ee';
         if (type === 'COMPOSITION') return '#f59e0b';
         if (type === 'AGGREGATION') return '#10b981';
         return '#94a3b8';
      }
      
      // Zoom Logic Setup
      setTimeout(() => {
          let zoomLevel = 1.0;
          const contentArea = document.getElementById('uml-content-area');
          const zOut = document.getElementById('btn-zoom-out');
          const zIn = document.getElementById('btn-zoom-in');
          const zRes = document.getElementById('btn-zoom-reset');
          const scrollArea = document.getElementById('uml-scroll-area');
          
          if(!contentArea || !zOut || !zIn || !zRes) return;
          
          const updateZoom = () => {
              contentArea.style.transform = `scale(${zoomLevel})`;
              zRes.innerText = Math.round(zoomLevel * 100) + '%';
          };
          
          zOut.onclick = () => { zoomLevel = Math.max(0.2, zoomLevel - 0.1); updateZoom(); };
          zIn.onclick = () => { zoomLevel = Math.min(3.0, zoomLevel + 0.1); updateZoom(); };
          zRes.onclick = () => { zoomLevel = 1.0; updateZoom(); };
          
          if(scrollArea) {
              scrollArea.addEventListener('wheel', (e) => {
                  if (e.ctrlKey || e.metaKey) {
                      e.preventDefault();
                      if (e.deltaY > 0) zoomLevel = Math.max(0.2, zoomLevel - 0.02);
                      else zoomLevel = Math.min(3.0, zoomLevel + 0.02);
                      updateZoom();
                  }
              });
          }
      }, 150);
    }
  },
  er: {
    render(container, erData, isEERDMode = false) {
      if (!erData || !erData.erDiagram || !container) return;
      const diagram = erData.erDiagram;
      const suffix = container.id || 'er';
      container.innerHTML = `
        <div id="${suffix}-zoom-controls" style="position:absolute; bottom:20px; right:20px; z-index:20; background:var(--surface); border:1px solid var(--border); border-radius:6px; display:flex; gap:1px; box-shadow:var(--shadow);">
            <button id="btn-zoom-out-${suffix}" style="padding:6px 12px; background:none; border:none; color:white; cursor:pointer; font-size:16px;">➖</button>
            <button id="btn-zoom-reset-${suffix}" style="padding:6px 12px; background:none; border:none; color:var(--text2); cursor:pointer; font-size:12px; font-weight:bold; min-width:60px;">100%</button>
            <button id="btn-zoom-in-${suffix}" style="padding:6px 12px; background:none; border:none; color:white; cursor:pointer; font-size:16px;">➕</button>
        </div>
        <div id="${suffix}-scroll-area" style="width:100%; height:100%; overflow:auto; position:relative; background:var(--bg);">
           <div id="${suffix}-content-area" style="position:relative; width:5000px; height:5000px; transform-origin: top left; background:var(--bg); transition: transform 0.1s ease-out;">
              <svg id="${suffix}-svg" style="position:absolute; top:0; left:0; width:100%; height:100%; pointer-events:none; z-index:1; overflow:visible;"></svg>
              <div id="${suffix}-boxes" style="position:absolute; top:0; left:0; width:100%; height:100%; z-index:2;"></div>
           </div>
        </div>
      `;
      const boxContainer = document.getElementById(`${suffix}-boxes`);
      const svg = document.getElementById(`${suffix}-svg`);
      const contentArea = document.getElementById(`${suffix}-content-area`);
      
      // Zoom Logic for ER/EERD
      let zoomLevel = 1.0;
      const zOut = document.getElementById(`btn-zoom-out-${suffix}`);
      const zIn = document.getElementById(`btn-zoom-in-${suffix}`);
      const zRes = document.getElementById(`btn-zoom-reset-${suffix}`);
      const scrollArea = document.getElementById(`${suffix}-scroll-area`);

      const updateZoom = () => {
          if(contentArea) contentArea.style.transform = `scale(${zoomLevel})`;
          if(zRes) zRes.innerText = Math.round(zoomLevel * 100) + '%';
      };
      
      if(zOut) zOut.onclick = () => { zoomLevel = Math.max(0.2, zoomLevel - 0.1); updateZoom(); };
      if(zIn) zIn.onclick = () => { zoomLevel = Math.min(3.0, zoomLevel + 0.1); updateZoom(); };
      if(zRes) zRes.onclick = () => { zoomLevel = 1.0; updateZoom(); };
      
      if(scrollArea) {
          scrollArea.addEventListener('wheel', (e) => {
              if (e.ctrlKey || e.metaKey) {
                  e.preventDefault();
                  zoomLevel = Math.max(0.2, Math.min(3.0, zoomLevel + (e.deltaY < 0 ? 0.05 : -0.05)));
                  updateZoom();
              }
          });
      }

      // Helper to find position by name (case-insensitive)
      const getPos = (name) => {
          const key = Object.keys(positions).find(k => k.toLowerCase() === name.toLowerCase());
          return key ? positions[key] : null;
      };
      
      const positions = {};
      const entityCols = {}; // Track which grid column each entity is in
      const cols = Math.max(1, Math.ceil(Math.sqrt(diagram.entities.length)));

      const getEERDBoxWidth = (ent) => {
          if (!ent) return 150;
          const maxAttrLen = (ent.attributes||[]).length > 0 ? Math.max(...(ent.attributes||[]).map(a => (a.name + (a.type ? ': '+a.type : '')).length)) : 0;
          return Math.max(160, 60 + Math.max(ent.name.length, maxAttrLen) * 8);
      };
      
      const getERBoxWidth = (ent) => {
          if (!ent || !ent.name) return 150;
          return Math.max(150, ent.name.length * 8.5 + 40);
      };

      // Pre-calculate entity box heights for EERD mode
      const entityBoxHeights = {};
      diagram.entities.forEach(ent => {
          if (isEERDMode) {
              // UML box: header(~35px) + attributes(~22px each) + padding(~18px)
              const attrCount = (ent.attributes||[]).length;
              entityBoxHeights[ent.name] = Math.max(60, 35 + Math.max(1, attrCount) * 22 + 18);
          } else {
              entityBoxHeights[ent.name] = 44; // Standard ER entity rect height
          }
      });

      const getTotalHalfHeight = (ent) => {
          if (!ent) return 24;
          if (isEERDMode) {
              return (entityBoxHeights[ent.name] || 100) / 2.0;
          } else {
              const attrCount = (ent.attributes || []).length;
              const attrSpan = (attrCount <= 1) ? 0 : (attrCount - 1) * 42.0;
              return Math.max(20, (attrSpan + 36) / 2.0);
          }
      };

      const spacingX = 1200; // Much wider to prevent line crossings through attributes
      let spacingY = 0;
      diagram.entities.forEach(e => {
          spacingY = Math.max(spacingY, getTotalHalfHeight(e) * 2 + 250);
      });
      spacingY = Math.max(spacingY, 700);

      // Removed getEdgePoint and elbowPath. Replaced by Universal Gutter Lane Router.

      let lineSvg = ''; // Lines layer (behind)
      let shapeSvg = ''; // Diamonds + labels layer (on top)

      let maxChildren = 0;
      if (isEERDMode) {
          diagram.entities.forEach(e => {
              if (e.hasSpecialization && e.childEntities) {
                  maxChildren = Math.max(maxChildren, e.childEntities.length);
              }
          });
      }
      const eerdChildSpacing = 400;
      const requiredLeftMargin = Math.max(350, (maxChildren > 1 ? ((maxChildren - 1) * eerdChildSpacing) / 2 + 200 : 350));
      const startX = isEERDMode ? requiredLeftMargin : 350;

      if (!isEERDMode) {
        // ═══ STANDARD ER LAYOUT ═══
        diagram.entities.forEach((ent, i) => {
           const row = Math.floor(i / cols), col = i % cols;
           const bx = startX + col * spacingX, by = 280 + row * spacingY;
           positions[ent.name] = [bx, by];
           entityCols[ent.name] = col;
        });
      } else {
        // ═══ EERD HIERARCHICAL LAYOUT ═══
        const parentEnts = diagram.entities.filter(e => e.hasSpecialization && e.childEntities && e.childEntities.length > 0);
        const childNames = new Set();
        parentEnts.forEach(p => (p.childEntities||[]).forEach(c => childNames.add(c)));
        const standaloneEnts = diagram.entities.filter(e => !childNames.has(e.name) && !parentEnts.includes(e));

        const standaloneCols = Math.max(1, Math.min(3, standaloneEnts.length));
        standaloneEnts.forEach((ent, i) => {
           const col = i % standaloneCols;
           const bx = startX + col * spacingX, by = 280 + Math.floor(i / standaloneCols) * spacingY;
           positions[ent.name] = [bx, by];
           entityCols[ent.name] = col;
        });

        let groupY = 280 + (standaloneEnts.length > 0 ? (Math.ceil(standaloneEnts.length/standaloneCols)*spacingY + 50) : 0);
        const parentSpacingX = spacingX;
        
        parentEnts.forEach((parent, pi) => {
           const px = startX + pi * parentSpacingX, py = groupY;
           positions[parent.name] = [px, py];
           entityCols[parent.name] = pi % 2;

           const centerX = px;
           const parentTotalH = getTotalHalfHeight(parent) * 2;
           const isaY = py + parentTotalH / 2 + 40; 
           
           // Draw ISA triangle
           const triCont = document.createElement('div');
           triCont.style.cssText = `position:absolute; left:${centerX-25}px; top:${isaY}px; width:50px; height:40px; z-index:15;`;
           const specType = parent.isDisjointSpecialization ? 'd' : 'o';
           triCont.innerHTML = `<svg width="50" height="40" viewBox="0 0 50 40"><polygon points="25,2 48,38 2,38" fill="var(--surface)" stroke="#a78bfa" stroke-width="2"/><text x="25" y="30" fill="#a78bfa" font-size="9" font-weight="bold" text-anchor="middle">ISA</text></svg>`;
           boxContainer.appendChild(triCont);
           // Line from parent box bottom to ISA triangle top
           lineSvg += `<line x1="${centerX}" y1="${py + parentTotalH/2}" x2="${centerX}" y2="${isaY}" stroke="#a78bfa" stroke-width="2" />`;
           shapeSvg += `<text x="${centerX+30}" y="${isaY+22}" fill="#a78bfa" font-size="10" font-weight="bold">${specType}</text>`;

           const children = (parent.childEntities||[]).map(cn => diagram.entities.find(e => e.name === cn)).filter(Boolean);
           const childSpacing = 400;
           const childStartX = centerX - ((children.length-1) * childSpacing) / 2;
           children.forEach((child, ci) => {
              const cx = childStartX + ci * childSpacing;
              const childTotalH = getTotalHalfHeight(child) * 2;
              const cy = isaY + 80 + childTotalH/2; // Below ISA triangle
              positions[child.name] = [cx, cy];
              entityCols[child.name] = ci % 2;
              // Line from ISA triangle bottom to child box top
              lineSvg += `<line x1="${centerX}" y1="${isaY+40}" x2="${cx}" y2="${cy - childTotalH/2}" stroke="#a78bfa" stroke-width="1.5" />`;
           });
           
           if (parentEnts.length > 2 && pi % 2 === 1) groupY += spacingY + 200;
        });
      }

      // ═══ DRAW RELATIONSHIPS WITH UNIVERSAL GUTTER LANE ROUTING ═══
      // Track used label positions to avoid overlap
      let usedLabelSlots = [];
      let entityExitCounts = {}; // Track exits to stagger X coordinates
      
      diagram.relationships.forEach((rel, relIdx) => {
         if (isEERDMode) {
             const parentEnts = diagram.entities.filter(e => e.hasSpecialization && e.childEntities && e.childEntities.length > 0);
             const childNames = new Set();
             parentEnts.forEach(p => (p.childEntities||[]).forEach(c => childNames.add(c)));
             const isISA = (childNames.has(rel.entity1) && parentEnts.some(p => p.name === rel.entity2)) ||
                           (childNames.has(rel.entity2) && parentEnts.some(p => p.name === rel.entity1));
             if (isISA) return; // Already drawn above
         }
         
         const p1 = positions[rel.entity1], p2 = positions[rel.entity2];
         if (!p1 || !p2) return;
         
         const bx1 = p1[0], by1 = p1[1];
         const bx2 = p2[0], by2 = p2[1];
         
         const ent1 = diagram.entities.find(e => e.name === rel.entity1);
         const ent2 = diagram.entities.find(e => e.name === rel.entity2);
         const totalHH1 = getTotalHalfHeight(ent1);
         const totalHH2 = getTotalHalfHeight(ent2);
         
         const coreHH1 = isEERDMode ? totalHH1 : 20;
         const coreHH2 = isEERDMode ? totalHH2 : 20;
         
         let pathD1, pathD2, mx, my;
         let exit1, exit2;
         
         // Helper to calculate a safe edge point that exits top or bottom
         const getVerticalExit = (bx, by, hh, targetY) => {
             return targetY > by ? { x: bx, y: by + hh } : { x: bx, y: by - hh };
         };
         
         if (rel.entity1 === rel.entity2) {
             // Self-referencing: Draw a loop avoiding the entity box
             const hw = isEERDMode ? getEERDBoxWidth(ent1)/2 + 10 : getERBoxWidth(ent1)/2 + 10;
             pathD1 = `M ${bx1 + hw} ${by1} L ${bx1 + hw + 40} ${by1} L ${bx1 + hw + 40} ${by1 - coreHH1 - 40}`;
             pathD2 = `M ${bx1} ${by1 - coreHH1} L ${bx1} ${by1 - coreHH1 - 40} L ${bx1 + hw + 40} ${by1 - coreHH1 - 40}`;
             mx = bx1 + hw + 40;
             my = by1 - coreHH1 - 40;
             exit1 = { x: bx1 + hw, y: by1 };
             exit2 = { x: bx1, y: by1 - coreHH1 };
         } else {
             // Track exit counts to stagger vertical drops so they never overlap
             let c1 = entityExitCounts[rel.entity1] || 0;
             let c2 = entityExitCounts[rel.entity2] || 0;
             entityExitCounts[rel.entity1] = c1 + 1;
             entityExitCounts[rel.entity2] = c2 + 1;
             
             // All lines exit from the exact center to form a single clean vertical trunk
             let offsetX1 = 0; 
             let offsetX2 = 0;
             
             // Universal horizontal lane routing
             let laneY;
             if (Math.abs(by1 - by2) < 10) {
                 // Same row: route in a lane completely below the entity bounds
                 const maxTotalH = Math.max(totalHH1, totalHH2);
                 laneY = by1 + maxTotalH + 70 + (relIdx % 5) * 50;
             } else {
                 // Different rows: route strictly in the safe gap between them
                 let topY = Math.min(by1, by2);
                 let botY = Math.max(by1, by2);
                 let topTotalH = (topY === by1) ? totalHH1 : totalHH2;
                 let botTotalH = (botY === by1) ? totalHH1 : totalHH2;
                 
                 let safeTop = topY + topTotalH + 50;
                 let safeBottom = botY - botTotalH - 50;
                 
                 // If gap is too small, just use midpoint
                 if (safeBottom <= safeTop) {
                     laneY = (topY + botY) / 2;
                 } else {
                     // Distribute evenly within the safe zone
                     let gap = safeBottom - safeTop;
                     let fraction = 0.1 + (relIdx % 6) * 0.15; // Support up to 6 safe lanes
                     laneY = safeTop + gap * fraction;
                 }
             }
             
             exit1 = getVerticalExit(bx1 + offsetX1, by1, coreHH1, laneY);
             exit2 = getVerticalExit(bx2 + offsetX2, by2, coreHH2, laneY);
             
             mx = (exit1.x + exit2.x) / 2;
             my = laneY;
             
             // Draw L-shape paths exiting vertically to the lane, then horizontally to the midpoint
             pathD1 = `M ${exit1.x} ${exit1.y} L ${exit1.x} ${laneY} L ${mx} ${laneY}`;
             pathD2 = `M ${exit2.x} ${exit2.y} L ${exit2.x} ${laneY} L ${mx} ${laneY}`;
         }
         
         lineSvg += `<path d="${pathD1}" fill="none" stroke="#94a3b8" stroke-width="${rel.isIdentifying?3:1.8}" />`;
         lineSvg += `<path d="${pathD2}" fill="none" stroke="#94a3b8" stroke-width="${rel.isIdentifying?3:1.8}" />`;
         
         // Diamond shape (on top layer)
         const ds = 28;
         shapeSvg += `<polygon points="${mx},${my-ds} ${mx+ds*1.3},${my} ${mx},${my+ds} ${mx-ds*1.3},${my}" fill="#1e293b" stroke="${rel.isIdentifying?'#f59e0b':'#10b981'}" stroke-width="${rel.isIdentifying?3:2}" />`;
         if (rel.isIdentifying) {
             const ids = ds - 6;
             shapeSvg += `<polygon points="${mx},${my-ids} ${mx+ids*1.3},${my} ${mx},${my+ids} ${mx-ids*1.3},${my}" fill="none" stroke="#f59e0b" stroke-width="1.5" />`;
         }
         
         shapeSvg += `<text x="${mx}" y="${my+5}" fill="white" font-size="11" font-weight="bold" text-anchor="middle" font-family="Inter, sans-serif">${rel.name || 'rel'}</text>`;
         
         // Cardinality labels
         let isLoop = rel.entity1 === rel.entity2;
         let e1x = isLoop ? bx1 + (isEERDMode ? getEERDBoxWidth(ent1)/2 + 10 : getERBoxWidth(ent1)/2 + 10) : exit1.x;
         let e1y = isLoop ? by1 : exit1.y;
         let e2x = isLoop ? bx1 : exit2.x;
         let e2y = isLoop ? by1 - coreHH1 : exit2.y;
         
         let l1x, l1y, l2x, l2y;
         if (isLoop) {
             l1y = e1y;
             l2y = e2y - 25;
             l1x = e1x + 20;
             l2x = e2x - 20;
         } else {
             if (Math.abs(mx - e1x) < 5) {
                 // Perfectly vertical entry to the diamond
                 l1x = e1x + 45;
                 l1y = my + (my > e1y ? -60 : 60);
             } else {
                 // Horizontal entry to the diamond
                 l1y = my - 22;
                 // Default: 45px away from trunk towards diamond
                 l1x = e1x + (mx > e1x ? 45 : -45);
                 // If horizontal branch is too short (< 110px), flip label to the OUTSIDE of the trunk
                 if (Math.abs(mx - e1x) < 110) {
                     l1x = e1x + (mx > e1x ? -45 : 45);
                 }
             }
             
             if (Math.abs(mx - e2x) < 5) {
                 // Perfectly vertical entry to the diamond
                 l2x = e2x + 45;
                 l2y = my + (my > e2y ? -60 : 60);
             } else {
                 // Horizontal entry to the diamond
                 l2y = my - 22;
                 // Default: 45px away from trunk towards diamond
                 l2x = e2x + (mx > e2x ? 45 : -45);
                 // If horizontal branch is too short (< 110px), flip label to the OUTSIDE of the trunk
                 if (Math.abs(mx - e2x) < 110) {
                     l2x = e2x + (mx > e2x ? -45 : 45);
                 }
             }
         }
         
         shapeSvg += `<rect x="${l1x-24}" y="${l1y-11}" width="48" height="22" rx="4" fill="#1e293b" stroke="#334155" stroke-width="1" />`;
         shapeSvg += `<text x="${l1x}" y="${l1y+4}" fill="var(--red)" font-size="12" font-weight="bold" text-anchor="middle" font-family="Inter, sans-serif">${rel.card1}</text>`;
         
         shapeSvg += `<rect x="${l2x-24}" y="${l2y-11}" width="48" height="22" rx="4" fill="#1e293b" stroke="#334155" stroke-width="1" />`;
         shapeSvg += `<text x="${l2x}" y="${l2y+4}" fill="var(--red)" font-size="12" font-weight="bold" text-anchor="middle" font-family="Inter, sans-serif">${rel.card2}</text>`;
      });

      // ═══ DRAW ENTITIES (Box + Ellipses) ═══
      function drawEntity(ent, bx, by, col) {
         const boxW = getERBoxWidth(ent);
         const box = document.createElement('div');
         box.style.cssText = `position:absolute; left:${bx-boxW/2}px; top:${by-22}px; width:${boxW}px; height:44px; background:var(--surface); border:2px solid ${ent.isWeak?'var(--yellow)':'var(--accent)'}; border-radius:6px; display:flex; align-items:center; justify-content:center; color:var(--text); font-weight:bold; font-family:var(--font); z-index:15; box-shadow:0 2px 8px rgba(0,0,0,0.3);`;
         if (ent.isWeak) box.style.boxShadow = `inset 0 0 0 3px var(--surface), inset 0 0 0 5px var(--yellow), 0 2px 8px rgba(0,0,0,0.3)`;
         box.innerText = ent.name;
         boxContainer.appendChild(box);

         const allAttrs = (ent.attributes||[]);
         const nonMV = allAttrs.filter(a => !a.isMultiValued);
         const mvAttrs = allAttrs.filter(a => a.isMultiValued);
         const totalAttrs = nonMV.length + mvAttrs.length;
         
         // Desktop uses 42px spacing - match it exactly
         const attrSpacing = 42;
         const attrStartY = by - ((totalAttrs - 1) * attrSpacing) / 2;
         const attrSide = (col % 2 === 0) ? 1 : -1;
         // Push attributes far enough from entity to not overlap with relationship diamonds
         const entityEdgeX = bx + attrSide * (boxW / 2);

         nonMV.forEach((attr, ai) => {
            const textW = attr.name.length * 6;
            const ew = Math.max(104, textW + 30);
            const ax = bx + attrSide * (boxW / 2 + 40 + ew/2), ay = attrStartY + ai * attrSpacing;
            lineSvg += `<line x1="${entityEdgeX}" y1="${by}" x2="${ax}" y2="${ay}" stroke="#475569" stroke-width="1.2" />`;
            const el = document.createElement('div');
            const stroke = attr.isPrimaryKey ? 'var(--yellow)' : (attr.isForeignKey ? 'var(--red)' : (attr.isDerived ? '#6b7280' : 'var(--text3)'));
            const decor = attr.isPrimaryKey ? 'text-decoration:underline; font-weight:bold;' : '';
            const dashBorder = attr.isDerived ? 'border-style:dashed;' : '';
            el.style.cssText = `position:absolute; left:${ax-ew/2}px; top:${ay-15}px; width:${ew}px; height:30px; background:var(--surface); border:1.5px solid ${stroke}; border-radius:50%; display:flex; align-items:center; justify-content:center; color:var(--text); font-size:10px; z-index:16; ${decor} ${dashBorder}`;
            el.innerText = attr.name;
            boxContainer.appendChild(el);
         });

         mvAttrs.forEach((attr, ai) => {
            const textW = attr.name.length * 6;
            const ew = Math.max(104, textW + 30);
            const ax = bx + attrSide * (boxW / 2 + 40 + ew/2), ay = attrStartY + (nonMV.length + ai) * attrSpacing;
            lineSvg += `<line x1="${entityEdgeX}" y1="${by}" x2="${ax}" y2="${ay}" stroke="#a78bfa" stroke-width="1" />`;
            const el = document.createElement('div');
            el.style.cssText = `position:absolute; left:${ax-ew/2}px; top:${ay-15}px; width:${ew}px; height:30px; background:var(--surface); border:2px solid #a78bfa; border-radius:50%; display:flex; align-items:center; justify-content:center; color:var(--text); font-size:10px; z-index:16; box-shadow: inset 0 0 0 3px var(--surface), inset 0 0 0 4.5px #a78bfa;`;
            el.innerText = attr.name;
            boxContainer.appendChild(el);
         });
      }

      diagram.entities.forEach(ent => {
          const pos = positions[ent.name];
          if(pos) {
              if (isEERDMode) {
                  const h = entityBoxHeights[ent.name] || 100;
                  const w = getEERDBoxWidth(ent);
                  // Center the EERD box vertically and horizontally
                  makeEERDBox(ent, pos[0] - w/2, pos[1] - h/2, w, h);
              } else {
                  drawEntity(ent, pos[0], pos[1], entityCols[ent.name] || 0);
              }
          }
      });

      svg.innerHTML = lineSvg + shapeSvg;

      function makeEERDBox(ent, x, y, w, h) {
         const box = document.createElement('div');
         box.style.cssText = `position:absolute; left:${x}px; top:${y}px; min-width:${w}px; min-height:${h}px; background:#1e293b; border:2px solid ${ent.isWeak?'#f59e0b':'#3b82f6'}; border-radius:6px; font-family:var(--font); z-index:10; box-shadow:0 2px 8px rgba(0,0,0,0.3);`;
         if (ent.isWeak) box.style.boxShadow = `inset 0 0 0 3px #1e293b, inset 0 0 0 5px #f59e0b, 0 2px 8px rgba(0,0,0,0.3)`;
         let html = `<div style="padding:8px 12px; text-align:center; border-bottom:2px solid #334155; font-weight:bold; color:white; font-size:13px; background:#1a2332; border-radius:4px 4px 0 0;">${ent.name}</div>`;
         html += `<div style="padding:6px 12px; font-size:11px; font-family:'Consolas',monospace;">`;
         (ent.attributes||[]).forEach(a => {
            let color = a.isPrimaryKey ? '#fbbf24' : (a.isForeignKey ? '#f87171' : '#e2e8f0');
            let decoration = a.isPrimaryKey ? 'text-decoration:underline;' : '';
            let style = a.isDerived ? 'font-style:italic; border-bottom:1px dashed #6b7280;' : '';
            let prefix = a.isMultiValued ? '{MV} ' : (a.isDerived ? '/ ' : '');
            html += `<div style="color:${color}; padding:3px 0; ${decoration} ${style}">${prefix}${a.name}${a.type ? ': '+a.type : ''}</div>`;
         });
         if (!ent.attributes || ent.attributes.length === 0) html += `<div style="color:#64748b; font-style:italic;">(no attributes)</div>`;
         html += `</div>`;
         box.innerHTML = html;
         boxContainer.appendChild(box);
         return box;
      }
    }
  },
  relational: {
    render(container, relSchema) {
      if (!relSchema || !relSchema.tables || !container) return;
      const suffix = container.id || 'rel';
      container.innerHTML = `
        <div id="${suffix}-zoom-controls" style="position:absolute; bottom:20px; right:20px; z-index:20; background:var(--surface); border:1px solid var(--border); border-radius:6px; display:flex; gap:1px; box-shadow:var(--shadow);">
            <button id="btn-zoom-out-${suffix}" style="padding:6px 12px; background:none; border:none; color:white; cursor:pointer; font-size:16px;">➖</button>
            <button id="btn-zoom-reset-${suffix}" style="padding:6px 12px; background:none; border:none; color:var(--text2); cursor:pointer; font-size:12px; font-weight:bold; min-width:60px;">100%</button>
            <button id="btn-zoom-in-${suffix}" style="padding:6px 12px; background:none; border:none; color:white; cursor:pointer; font-size:16px;">➕</button>
        </div>
        <div id="${suffix}-scroll-area" style="width:100%; height:100%; overflow:auto; position:relative; background:var(--bg);">
           <div id="${suffix}-content-area" style="position:relative; width:5000px; height:5000px; transform-origin: top left; background:var(--bg); transition: transform 0.1s ease-out;">
              <svg id="${suffix}-svg" style="position:absolute; top:0; left:0; width:100%; height:100%; pointer-events:none; z-index:1; overflow:visible;"></svg>
              <div id="${suffix}-boxes" style="position:absolute; top:0; left:0; width:100%; height:100%; z-index:2;"></div>
           </div>
        </div>
      `;
      const boxContainer = document.getElementById(`${suffix}-boxes`);
      const svg = document.getElementById(`${suffix}-svg`);
      const contentArea = document.getElementById(`${suffix}-content-area`);

      // Zoom Logic for Relational
      let zoomLevel = 1.0;
      const zOut = document.getElementById(`btn-zoom-out-${suffix}`);
      const zIn = document.getElementById(`btn-zoom-in-${suffix}`);
      const zRes = document.getElementById(`btn-zoom-reset-${suffix}`);
      const scrollArea = document.getElementById(`${suffix}-scroll-area`);

      const updateZoom = () => {
          if(contentArea) contentArea.style.transform = `scale(${zoomLevel})`;
          if(zRes) zRes.innerText = Math.round(zoomLevel * 100) + '%';
      };
      
      if(zOut) zOut.onclick = () => { zoomLevel = Math.max(0.2, zoomLevel - 0.1); updateZoom(); };
      if(zIn) zIn.onclick = () => { zoomLevel = Math.min(3.0, zoomLevel + 0.1); updateZoom(); };
      if(zRes) zRes.onclick = () => { zoomLevel = 1.0; updateZoom(); };
      
      if(scrollArea) {
          scrollArea.addEventListener('wheel', (e) => {
              if (e.ctrlKey || e.metaKey) {
                  e.preventDefault();
                  zoomLevel = Math.max(0.2, Math.min(3.0, zoomLevel + (e.deltaY < 0 ? 0.05 : -0.05)));
                  updateZoom();
              }
          });
      }
      
      const tableNodes = {};
      const columnNodes = {};
      let currentY = 50;
      let startX = 50;
      const tableSpacing = 140; // Match desktop RelationalMappingCanvas spacing

      relSchema.tables.forEach(table => {
         const tableContainer = document.createElement('div');
         tableContainer.style.cssText = `position:absolute; left:${startX}px; top:${currentY}px; display:flex; flex-direction:column; gap:6px;`;
         
         const nameLabel = document.createElement('div');
         nameLabel.style.cssText = `color:var(--text3); font-size:13px; font-weight:bold; font-family:var(--font); text-transform:uppercase; letter-spacing:0.5px;`;
         nameLabel.innerText = table.name;
         tableContainer.appendChild(nameLabel);
         
         const columnsRow = document.createElement('div');
         columnsRow.style.cssText = `display:flex; background:var(--surface); border:2px solid var(--border); border-radius:4px; overflow:hidden;`;
         
         const colMap = {};
         table.columns.forEach((col, i) => {
            const cell = document.createElement('div');
            cell.style.cssText = `padding:10px 18px; min-width:110px; display:flex; align-items:center; justify-content:center; border-right:1px solid var(--border);`;
            if (i === table.columns.length - 1) cell.style.borderRight = 'none';
            
            const label = document.createElement('div');
            label.style.cssText = `font-family:var(--font); font-weight:600; font-size:12px; white-space:nowrap;`;
            label.innerText = col.name;
            
            if (col.isPrimaryKey) {
                label.style.color = 'var(--yellow)';
                label.style.textDecoration = 'underline';
            } else if (col.isForeignKey) {
                label.style.color = 'var(--red)';
            } else {
                label.style.color = 'var(--text)';
            }
            
            cell.appendChild(label);
            columnsRow.appendChild(cell);
            colMap[col.name] = cell;
         });
         
         tableContainer.appendChild(columnsRow);
         boxContainer.appendChild(tableContainer);
         
         tableNodes[table.name] = tableContainer;
         columnNodes[table.name] = colMap;
         
         currentY += tableSpacing;
      });

      // ═══ IMPROVED FK Arrow Routing ═══
      // Uses wide gutter lanes, color-coded arrows, rounded corners, proper gap avoidance
      const arrowColors = ['#ef4444','#f97316','#eab308','#22c55e','#06b6d4','#8b5cf6','#ec4899','#3b82f6'];
      
      function drawArrows() {
          svg.innerHTML = '';
          let svgContent = '';
          let arrowIdx = 0;

          // Collect all table vertical ranges for gap avoidance
          const tableRanges = [];
          for (const tName in tableNodes) {
              const node = tableNodes[tName];
              tableRanges.push({ name: tName, top: node.offsetTop, bottom: node.offsetTop + node.offsetHeight });
          }
          tableRanges.sort((a, b) => a.top - b.top);

          // Max right edge for gutter base
          let maxRight = 0;
          for (const tName in tableNodes) {
              const node = tableNodes[tName];
              maxRight = Math.max(maxRight, node.offsetLeft + node.offsetWidth);
          }
          const gutterBase = maxRight + 60;
          const laneWidth = 22;

          // Build defs with per-color arrowheads
          let defs = '<defs>';
          arrowColors.forEach((color, ci) => {
              defs += `<marker id="ah-${suffix}-${ci}" markerWidth="8" markerHeight="6" refX="8" refY="3" orient="auto"><polygon points="0 0, 8 3, 0 6" fill="${color}" /></marker>`;
          });
          defs += '</defs>';

          // Find safe gap Y between two tables (midpoint of the gap)
          function findGapY(tableNode, isAbove, offset) {
              const tTop = tableNode.offsetTop;
              const tBot = tTop + tableNode.offsetHeight;
              if (isAbove) {
                  // Find the table directly above this one
                  let prevBot = 0;
                  for (const r of tableRanges) {
                      if (r.bottom <= tTop) prevBot = r.bottom;
                  }
                  return prevBot + (tTop - prevBot) / 2 + offset * 5;
              } else {
                  // Find the table directly below this one
                  let nextTop = tBot + 200;
                  for (const r of tableRanges) {
                      if (r.top > tBot) { nextTop = r.top; break; }
                  }
                  return tBot + (nextTop - tBot) / 2 + offset * 5;
              }
          }

          // Rounded corner path builder
          function buildRoundedPath(points, radius) {
              if (points.length < 2) return '';
              let d = `M ${points[0][0]} ${points[0][1]}`;
              for (let j = 1; j < points.length - 1; j++) {
                  const prev = points[j - 1], cur = points[j], next = points[j + 1];
                  const dxI = cur[0] - prev[0], dyI = cur[1] - prev[1];
                  const dxO = next[0] - cur[0], dyO = next[1] - cur[1];
                  const lI = Math.hypot(dxI, dyI), lO = Math.hypot(dxO, dyO);
                  if (lI < 2 || lO < 2) { d += ` L ${cur[0]} ${cur[1]}`; continue; }
                  const r = Math.min(radius, lI / 2, lO / 2);
                  const bx = cur[0] - (dxI / lI) * r, by = cur[1] - (dyI / lI) * r;
                  const ax = cur[0] + (dxO / lO) * r, ay = cur[1] + (dyO / lO) * r;
                  d += ` L ${bx} ${by} Q ${cur[0]} ${cur[1]} ${ax} ${ay}`;
              }
              d += ` L ${points[points.length - 1][0]} ${points[points.length - 1][1]}`;
              return d;
          }

          relSchema.tables.forEach(table => {
             if (!columnNodes[table.name]) return;
             table.columns.forEach(col => {
                if (!col.isForeignKey) return;
                const targetTableName = col.referencedTable;
                if (!targetTableName) return;
                
                const srcTableNode = tableNodes[table.name];
                const srcColMap = columnNodes[table.name];
                const srcColCell = srcColMap ? srcColMap[col.name] : null;
                
                const targetKey = Object.keys(tableNodes).find(k => k.toLowerCase() === targetTableName.toLowerCase());
                const targetTableNode = targetKey ? tableNodes[targetKey] : null;
                const targetColMap = targetKey ? columnNodes[targetKey] : null;
                const targetColCell = targetColMap ? (targetColMap[col.referencedColumn] || targetColMap[Object.keys(targetColMap)[0]]) : null;
                
                if (!srcTableNode || !srcColCell || !targetTableNode || !targetColCell) return;
                
                const targetIsAbove = targetTableNode.offsetTop < srcTableNode.offsetTop;
                const color = arrowColors[arrowIdx % arrowColors.length];
                const markerRef = `ah-${suffix}-${arrowIdx % arrowColors.length}`;

                // Source: center of FK cell, exit vertically
                const srcRow = srcTableNode.children[1];
                const sx = srcTableNode.offsetLeft + srcRow.offsetLeft + srcColCell.offsetLeft + srcColCell.offsetWidth / 2;
                const sy = srcTableNode.offsetTop + srcRow.offsetTop + srcColCell.offsetTop + (targetIsAbove ? 0 : srcColCell.offsetHeight);

                // Target: center of PK cell, enter vertically
                const tgtRow = targetTableNode.children[1];
                const tx = targetTableNode.offsetLeft + tgtRow.offsetLeft + targetColCell.offsetLeft + targetColCell.offsetWidth / 2;
                const ty = targetTableNode.offsetTop + tgtRow.offsetTop + targetColCell.offsetTop + (targetIsAbove ? targetColCell.offsetHeight : 0);

                // Gap Y: safe midpoint between tables
                const srcGapY = findGapY(srcTableNode, targetIsAbove, arrowIdx);
                const tgtGapY = findGapY(targetTableNode, !targetIsAbove, arrowIdx);
                
                // Gutter lane X
                const gutterX = gutterBase + arrowIdx * 28; // wider lanes

                // Build 6-point orthogonal path with rounded corners
                const pts = [[sx, sy], [sx, srcGapY], [gutterX, srcGapY], [gutterX, tgtGapY], [tx, tgtGapY], [tx, ty]];
                const pathD = buildRoundedPath(pts, 10);
                
                svgContent += `<path d="${pathD}" fill="none" stroke="${color}" stroke-width="1.8" stroke-linecap="round" opacity="0.9" marker-end="url(#${markerRef})" />`;
                
                // FK label at gutter midpoint - rotated 90 deg so they NEVER overlap
                const labelY = (srcGapY + tgtGapY) / 2;
                const labelX = gutterX + 4;
                svgContent += `<text x="${labelX}" y="${labelY}" fill="${color}" font-size="10" font-family="var(--font)" font-weight="bold" opacity="0.8" text-anchor="middle" transform="rotate(90 ${labelX} ${labelY})">${col.name} → ${targetKey}</text>`;
                
                arrowIdx++;
             });
          });
          
          svg.innerHTML = defs + svgContent;
      }

      setTimeout(drawArrows, 200);
      setTimeout(drawArrows, 500);
      const relTabBtn = document.getElementById('rel-tab-btn');
      if (relTabBtn) relTabBtn.addEventListener('click', () => setTimeout(drawArrows, 200));
      document.querySelectorAll('.dtab').forEach(btn => {
          btn.addEventListener('click', () => { if (btn.dataset.tab === 'relational') setTimeout(drawArrows, 200); });
      });
    }
  }
};
