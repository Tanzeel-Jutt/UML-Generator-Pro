// Robust Parsers for UML Generator Pro Web

const parsers = {
  java: {
    parse(code) {
      const diagram = { classes: [], relationships: [] };
      if (!code || code.trim() === '') return diagram;
      
      // Clean comments, string literals, and annotations to prevent regex issues
      let cleanedCode = code.replace(/\/\*[\s\S]*?\*\/|\/\/.*/g, '')
                            .replace(/"(?:\\.|[^"\\])*"/g, '""')
                            .replace(/@\w+(?:\s*\([^)]*\))?/g, ' ');
      
      const classRegex = /(?:public|private|protected)?\s*(?:static|abstract|final\s+)*(class|interface|enum|record)\s+(\w+)(?:\s+extends\s+([\w<>\s,]+))?(?:\s+implements\s+([\w<>\s,]+))?\s*\{/g;
      
      const classBlocks = [];
      let match;
      
      while ((match = classRegex.exec(cleanedCode)) !== null) {
         if (classBlocks.length > 0) {
             classBlocks[classBlocks.length - 1].end = match.index;
         }
         classBlocks.push({
             name: match[2],
             parent: match[3] ? match[3].replace(/<.*>/g, '').trim() : null,
             interfaces: match[4] ? match[4].split(',').map(i => i.replace(/<.*>/g, '').trim()) : [],
             type: match[1] === 'interface' ? 'INTERFACE' : 
                   match[1] === 'enum' ? 'ENUM' :
                   match[1] === 'record' ? 'RECORD' : 'CLASS',
             start: match.index,
             bodyStart: classRegex.lastIndex
         });
      }
      
      if (classBlocks.length > 0) {
          classBlocks[classBlocks.length - 1].end = cleanedCode.length;
      }
      
      classBlocks.forEach(block => {
         const cls = {
            name: block.name,
            type: block.type,
            attributes: [],
            methods: [],
            constants: [],
            parent: block.parent,
            interfaces: block.interfaces,
            rawBody: cleanedCode.substring(block.bodyStart, block.end)
         };
         
         const fullBody = cleanedCode.substring(block.bodyStart, block.end);
         
         // Helper: Remove everything inside nested {} to avoid local variables and method calls
         let body = '';
         let depth = 0;
         for(let i=0; i<fullBody.length; i++){
             if(fullBody[i] === '{') {
                 if(depth === 0) body += '{';
                 depth++; 
                 continue; 
             }
             if(fullBody[i] === '}') { 
                 depth--; 
                 if(depth === 0) body += '}';
                 continue; 
             }
             if(depth === 0) body += fullBody[i];
         }
         
         if (cls.type === 'ENUM') {
            const enumMatch = body.match(/^\s*([A-Z0-9_,\s]+);/);
            if (enumMatch) {
               cls.constants = enumMatch[1].split(',').map(s => s.trim()).filter(s => s);
            }
         }
         
         // Extract fields: <access> <static> <final> <Type> <name> ;|=
         const fieldRegex = /(?:(public|private|protected)\s+)?(?:(static)\s+)?(?:(final)\s+)?([\w<>\[\],\s]+)\s+(\w+)\s*(?:=|;)/g;
         let fMatch;
         while ((fMatch = fieldRegex.exec(body)) !== null) {
             let access = fMatch[1];
             let isStatic = !!fMatch[2];
             let isFinal = !!fMatch[3];
             let type = fMatch[4].trim();
             let name = fMatch[5].trim();
             
             // Fix regex skipping optional groups due to leading spaces
             const typeParts = type.split(/\s+/);
             while(typeParts.length > 0 && ['public','private','protected','static','final'].includes(typeParts[0])) {
                 const p = typeParts.shift();
                 if (p === 'public' || p === 'private' || p === 'protected') access = p;
                 if (p === 'static') isStatic = true;
                 if (p === 'final') isFinal = true;
             }
             type = typeParts.join(' ');

             // Ignore false positives from method parameters or inside methods
             if (type !== 'return' && type !== 'new' && type !== 'else' && !name.includes('(')) {
                 cls.attributes.push({
                     access: access || 'private',
                     isStatic: isStatic,
                     isFinal: isFinal,
                     type: type,
                     name: name
                 });
             }
         }
         
         // Extract methods and constructors
         const methodRegex = /(?:(public|private|protected)\s+)?(?:(static)\s+)?(?:(abstract)\s+)?(?:(final)\s+)?([\w<>\[\],\s]*)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\s]+)?(?:\{|;)/g;
         let mMatch;
         while ((mMatch = methodRegex.exec(body)) !== null) {
             let access = mMatch[1];
             let isStatic = !!mMatch[2];
             let isAbstract = !!mMatch[3];
             let isFinal = !!mMatch[4];
             let returnType = mMatch[5] ? mMatch[5].trim() : '';
             let name = mMatch[6].trim();
             let params = mMatch[7].trim();
             
             const typeParts = returnType.split(/\s+/);
             while(typeParts.length > 0 && ['public','private','protected','static','abstract','final'].includes(typeParts[0])) {
                 const p = typeParts.shift();
                 if (p === 'public' || p === 'private' || p === 'protected') access = p;
                 if (p === 'static') isStatic = true;
                 if (p === 'abstract') isAbstract = true;
                 if (p === 'final') isFinal = true;
             }
             returnType = typeParts.join(' ');

             if (name === 'if' || name === 'for' || name === 'while' || name === 'catch' || name === 'switch') continue;
             
             let mBody = "";
             if (mMatch[0].endsWith('{')) {
                 let sigIndex = fullBody.indexOf(mMatch[0]);
                 if (sigIndex !== -1) {
                     let startIdx = sigIndex + mMatch[0].length - 1;
                     let depth = 0;
                     for (let i = startIdx; i < fullBody.length; i++) {
                         mBody += fullBody[i];
                         if (fullBody[i] === '{') depth++;
                         else if (fullBody[i] === '}') {
                             depth--;
                             if (depth === 0) break;
                         }
                     }
                 }
             }

             const isConstructor = name === cls.name && returnType === '';
             
             cls.methods.push({
                 access: access || 'public',
                 isStatic: isStatic,
                 isAbstract: isAbstract,
                 isFinal: isFinal,
                 isConstructor: isConstructor,
                 returnType: isConstructor ? '' : returnType,
                 name: name,
                 params: params,
                 rawBody: mBody
             });
         }
         
         diagram.classes.push(cls);
      });
      
      // Extract Relationships (Second Pass to ensure all classes are known)
      const classNames = new Set(diagram.classes.map(c => c.name));
      const collectionTypes = new Set(['List', 'ArrayList', 'Set', 'HashSet', 'Map', 'HashMap', 'Collection']);
      
      diagram.classes.forEach(cls => {
          if (cls.parent) {
              diagram.relationships.push({ source: cls.name, target: cls.parent, type: 'INHERITANCE' });
          }
          cls.interfaces.forEach(iface => {
              if (iface) diagram.relationships.push({ source: cls.name, target: iface, type: 'IMPLEMENTATION' });
          });
          
          cls.attributes.forEach(attr => {
              const typeBase = attr.type.replace(/[\[\]<>\s]/g, '').split(',')[0]; // Simplify
              const collectionMatch = attr.type.match(/<(.*?)>/);
              let innerType = collectionMatch ? collectionMatch[1].trim() : null;
              if(innerType && innerType.includes(',')) innerType = innerType.split(',').pop().trim(); // for Map<K, V> -> V
              
              if (classNames.has(typeBase)) {
                  diagram.relationships.push({ source: cls.name, target: typeBase, type: 'COMPOSITION' });
              } else if (collectionTypes.has(typeBase) && innerType && classNames.has(innerType)) {
                  diagram.relationships.push({ source: cls.name, target: innerType, type: 'AGGREGATION' });
              }
          });
      });
      
      // EXACT Java UMLClassDiagramGenerator Logic
      // 1. Mark abstract
      diagram.classes.forEach(cls => {
          if (cls.type === 'CLASS' && cls.methods.some(m => m.isAbstract)) cls.type = 'ABSTRACT_CLASS';
      });

      // 2. Remove UI elements and common keywords
      const isUI = (name, type) => {
          const n = (name || '').toLowerCase();
          let t = (type || '').toLowerCase().replace(/<.*>/g, '').replace(/\[\]/g, '').trim();
          if (n.endsWith('btn') || n.endsWith('button') || n.endsWith('panel') || 
              n.endsWith('layout') || n.endsWith('box') || n.endsWith('scene') || 
              n.endsWith('view') || n.endsWith('spacer') || n === 'sep' ||
              n.endsWith('card') || n.endsWith('list') || n.endsWith('area') || 
              n.endsWith('header') || n.endsWith('footer') || n === 's' ||
              n === 'break' || n === 'continue' || n === 'return' || 
              n === 'stream' || n === 'columns') return true;
          return t.endsWith('button') || t.endsWith('panel') || t.endsWith('pane') ||
                 t.endsWith('label') || t.endsWith('textfield') || t.endsWith('textarea') ||
                 t.endsWith('checkbox') || t.endsWith('radiobutton') || t.endsWith('combobox') ||
                 t.endsWith('menu') || t.endsWith('menubar') || t.endsWith('menuitem') ||
                 t.endsWith('toolbar') || t.endsWith('window') || t.endsWith('dialog') ||
                 t.endsWith('frame') || t.endsWith('canvas') || t.endsWith('imageview') ||
                 t.endsWith('scene') || t.endsWith('stage') || t.endsWith('slider') ||
                 t.endsWith('spinner') || t.endsWith('scrollpane') || t.endsWith('listview') ||
                 t.endsWith('tableview') || t === 'text' || t === 'font' ||
                 t === 'color' || t === 'graphics' || t === 'graphics2d' ||
                 t === 'jcomponent' || t === 'component' || t === 'container' ||
                 t === 'vbox' || t === 'hbox' || t === 'region' || 
                 t === 'separator' || t === 'rectangle' || t === 'circle' || t === 'shape';
      };

      diagram.classes.forEach(cls => {
          cls.attributes = cls.attributes.filter(a => !isUI(a.name, a.type));
          cls.methods = cls.methods.filter(m => !isUI(m.name, m.returnType));
          const nameLower = cls.name.toLowerCase();
          if (nameLower.includes('main') || nameLower.includes('runner') || nameLower.includes('test')) {
              cls.methods = cls.methods.filter(m => m.isConstructor || m.name === 'main');
          }
      });

      diagram.relationships = diagram.relationships.filter(rel => !isUI(null, rel.target) && !isUI(null, rel.source));

      // 3. Create placeholders for external references
      const finalClassNames = new Set(diagram.classes.map(c => c.name));
      diagram.relationships.forEach(rel => {
          if (!finalClassNames.has(rel.target)) {
              diagram.classes.push({
                  name: rel.target, type: 'CLASS', attributes: [], methods: [], constants: [], packageName: '(external)'
              });
              finalClassNames.add(rel.target);
          }
      });
      
      return diagram;
    }
  },
  sql: {
    parse(code) {
      const er = { entities: [], relationships: [], isEERD: false };
      const tableRegex = /CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?\s*\(([\s\S]+?)\)\s*;/gi;
      let match;
      
      const tables = [];
      while ((match = tableRegex.exec(code)) !== null) {
        const tableName = match[1];
        const body = match[2];
        const table = { name: tableName, columns: [], primaryKeys: [], foreignKeys: [] };
        
        const lines = body.split(/,(?![^\(]*\))/);
        lines.forEach(line => {
           line = line.trim();
           if (!line) return;
           
           if (line.toUpperCase().startsWith('PRIMARY KEY')) {
              const pkMatch = /PRIMARY\s+KEY\s*\(([^)]+)\)/i.exec(line);
              if (pkMatch) {
                 const pks = pkMatch[1].split(',').map(p => p.replace(/[`"']/g, '').trim());
                 table.primaryKeys.push(...pks);
              }
           } else if (line.toUpperCase().startsWith('FOREIGN KEY')) {
              const fkMatch = /FOREIGN\s+KEY\s*\(`?(\w+)`?\)\s*REFERENCES\s*`?(\w+)`?\s*\(`?(\w+)`?\)/i.exec(line);
              if (fkMatch) {
                 table.foreignKeys.push({ col: fkMatch[1], refTable: fkMatch[2], refCol: fkMatch[3] });
              }
           } else if (!line.toUpperCase().startsWith('CONSTRAINT') && !line.toUpperCase().startsWith('UNIQUE') && !line.toUpperCase().startsWith('CHECK') && !line.toUpperCase().startsWith('INDEX')) {
              const colMatch = /^`?(\w+)`?\s+(\w+(?:\([^)]*\))?)(?:\s+(.*?))?$/i.exec(line);
              if (colMatch) {
                 const name = colMatch[1];
                 const type = colMatch[2];
                 const extra = (colMatch[3] || '').toUpperCase();
                 const isPk = extra.includes('PRIMARY KEY');
                 const isFk = extra.includes('REFERENCES');
                 const col = {
                    name, type, 
                    isPrimaryKey: isPk,
                    isForeignKey: isFk,
                    isNotNull: extra.includes('NOT NULL') || isPk
                 };
                 table.columns.push(col);
                 if (isPk) table.primaryKeys.push(name);
                 if (isFk) {
                    const inlineFk = /REFERENCES\s*`?(\w+)`?\s*\(`?(\w+)`?\)/i.exec(extra);
                    if (inlineFk) table.foreignKeys.push({ col: name, refTable: inlineFk[1], refCol: inlineFk[2] });
                 }
              }
           }
        });
        tables.push(table);
      }
      
      // Convert tables to ER Entities
      tables.forEach(t => {
         const entity = {
            name: t.name,
            attributes: t.columns.map(c => {
               const fk = t.foreignKeys.find(f => f.col.toLowerCase() === c.name.toLowerCase());
               return {
                  name: c.name, type: c.type, 
                  isPrimaryKey: t.primaryKeys.some(pk => pk.toLowerCase() === c.name.toLowerCase()),
                  isForeignKey: !!fk,
                  referencedTable: fk ? fk.refTable : null,
                  referencedColumn: fk ? fk.refCol : null
               };
            }),
            primaryKeys: t.primaryKeys,
            isWeak: false,
            childEntities: []
         };
         er.entities.push(entity);
      });

      // Smart Reference Detection (Guess refTable if missing but col is FK)
      er.entities.forEach(entity => {
          entity.attributes.forEach(attr => {
              if (attr.isForeignKey && !attr.referencedTable) {
                  const guess = attr.name.toLowerCase().replace(/_id$/, '').replace(/s$/, '');
                  // Improved matching: check if entity name contains guess or vice versa
                  const match = er.entities.find(e => {
                      const ename = e.name.toLowerCase();
                      return ename === guess || ename === guess + 's' || ename === guess + 'es' || ename.includes(guess);
                  });
                  if (match) {
                      attr.referencedTable = match.name;
                      attr.referencedColumn = match.primaryKeys[0] || 'id';
                  }
              }
          });
      });

      // Detect EERD Features: Specialization (ISA)
      er.entities.forEach(entity => {
          const pkAsFk = entity.attributes.find(a => a.isPrimaryKey && a.isForeignKey);
          if (pkAsFk && entity.primaryKeys.length === 1) {
              const refTable = pkAsFk.referencedTable;
              if (refTable) {
                  const parent = er.entities.find(e => e.name.toLowerCase() === refTable.toLowerCase());
                  if (parent && parent.name !== entity.name) {
                      entity.isSpecializedChild = true;
                      entity.parentEntity = parent.name;
                      if (!parent.childEntities.includes(entity.name)) parent.childEntities.push(entity.name);
                      parent.hasSpecialization = true;
                      er.isEERD = true;
                  }
              }
          }
      });

      // Detect Weak Entities and Relationships
      tables.forEach(t => {
         t.foreignKeys.forEach(fk => {
            const isPk = t.primaryKeys.some(pk => pk.toLowerCase() === fk.col.toLowerCase());
            const ent = er.entities.find(e => e.name === t.name);
            if (isPk && t.primaryKeys.length > 1 && ent) {
               ent.isWeak = true;
            }
            
            const relName = fk.col.replace(/_id$/i, '').replace(/_/g, ' ');
            er.relationships.push({
               name: relName,
               entity1: t.name,
               entity2: fk.refTable,
               card1: isPk ? '1..N' : '1..1',
               card2: isPk ? '1..1' : '0..1',
               isIdentifying: isPk
            });
         });
      });

      // Detect Multi-Valued Attributes
      const toRemove = [];
      er.entities.forEach(entity => {
          const tDef = tables.find(t => t.name === entity.name);
          if (tDef && tDef.foreignKeys.length === 1 && tDef.columns.length <= 3 && !entity.isSpecializedChild) {
              const fk = tDef.foreignKeys[0];
              const nonFk = tDef.columns.find(c => !c.isForeignKey && !c.isPrimaryKey);
              if (nonFk) {
                  const parent = er.entities.find(e => e.name.toLowerCase() === fk.refTable.toLowerCase());
                  if (parent) {
                      parent.attributes.push({ name: nonFk.name, type: nonFk.type, isMultiValued: true });
                      toRemove.push(entity.name);
                      er.isEERD = true;
                  }
              }
          }
      });
      er.entities = er.entities.filter(e => !toRemove.includes(e.name));
      er.relationships = er.relationships.filter(r => !toRemove.includes(r.entity1) && !toRemove.includes(r.entity2));

      // Detect Derived Attributes
      const derivedPrefixes = ['total_', 'avg_', 'count_', 'sum_', 'max_', 'min_', 'age'];
      er.entities.forEach(entity => {
          entity.attributes.forEach(attr => {
              if (derivedPrefixes.some(p => attr.name.toLowerCase().startsWith(p))) {
                  attr.isDerived = true;
                  er.isEERD = true;
              }
          });
      });

      return { erDiagram: er };
    },
    mapToRelational(erDiagram) {
        if (!erDiagram) return { tables: [] };
        const schema = { tables: [] };

        erDiagram.entities.forEach(ent => {
            const table = {
                name: ent.name,
                columns: ent.attributes.map(a => ({
                    name: a.name,
                    type: a.type || 'INT',
                    isPrimaryKey: a.isPrimaryKey,
                    isForeignKey: a.isForeignKey,
                    referencedTable: a.referencedTable,
                    referencedColumn: a.referencedColumn
                }))
            };
            schema.tables.push(table);
        });

        // Add FKs for relationships
        erDiagram.relationships.forEach(rel => {
            const t1 = schema.tables.find(t => t.name === rel.entity1);
            const t2 = schema.tables.find(t => t.name === rel.entity2);
            if (!t1 || !t2) return;

            // 1:N mapping (FK in N side)
            if (rel.card1 === '1..N') {
               const exists = t1.columns.some(c => c.isForeignKey && c.referencedTable === rel.entity2);
               if (!exists) {
                   t1.columns.push({
                       name: rel.entity2.toLowerCase() + '_id',
                       type: 'INT',
                       isPrimaryKey: false,
                       isForeignKey: true,
                       referencedTable: rel.entity2,
                       referencedColumn: 'id'
                   });
               }
            }
        });

        return schema;
    },
    generateSQL(schema) {
        let sql = `-- Normalized SQL Script\n-- Generated by UML Generator Pro\n\n`;
        schema.tables.forEach(t => {
            sql += `CREATE TABLE ${t.name} (\n`;
            const cols = t.columns.map(c => {
                let s = `    ${c.name} ${c.type}`;
                if (c.isPrimaryKey) s += ' PRIMARY KEY';
                if (c.isForeignKey && c.referencedTable) s += ` REFERENCES ${c.referencedTable}(${c.referencedColumn || 'id'})`;
                return s;
            });
            sql += cols.join(',\n') + '\n);\n\n';
        });
        return sql;
    }
  },
  python: {
      parse(code) { return { classes: [], relationships: [] }; }
  },
  cpp: {
      parse(code) { return parsers.java.parse(code); }
  }
};
