// Main App Logic for UML Generator Pro Web

document.addEventListener('DOMContentLoaded', () => {
   // State
   const state = {
      language: 'Java',
      theme: 'dracula',
      tabs: [{ id: 1, name: 'Main', content: '' }],
      activeTabId: 1,
      diagrams: { uml: null, er: null }
   };

   // DOM Elements
   const els = {
      langSelect: document.getElementById('language-selector'),
      themeSelect: document.getElementById('theme-selector'),
      addBtn: document.getElementById('add-class-btn'),
      rmBtn: document.getElementById('remove-class-btn'),
      tabsHeader: document.getElementById('code-tabs-header'),
      tabsBody: document.getElementById('code-tabs-body'),
      genBtn: document.getElementById('generate-btn'),
      clearBtn: document.getElementById('clear-btn'),
      classList: document.getElementById('class-list'),
      dtabs: document.querySelectorAll('.dtab'),
      panels: document.querySelectorAll('.tab-panel'),
      status: document.getElementById('status-label'),
      umlCanvas: document.getElementById('uml-canvas'),
      erCanvas: document.getElementById('er-canvas'),
      eerdCanvas: document.getElementById('eerd-canvas'),
      relCanvas: document.getElementById('rel-canvas'),
      sqlOutput: document.getElementById('sql-output'),
      solidOutput: document.getElementById('solid-output')
   };

   // Init
   renderCodeTabs();
   
   // Events
   els.langSelect.addEventListener('change', (e) => {
      state.language = e.target.value;
      updateUIForLanguage();
   });

   els.themeSelect.addEventListener('change', (e) => {
      document.body.className = `theme-${e.target.value}`;
   });

   els.addBtn.addEventListener('click', () => {
      const id = Date.now();
      state.tabs.push({ id, name: `Class ${state.tabs.length + 1}`, content: '' });
      state.activeTabId = id;
      renderCodeTabs();
   });

   els.rmBtn.addEventListener('click', () => {
      if (state.tabs.length > 1) {
         state.tabs = state.tabs.filter(t => t.id !== state.activeTabId);
         state.activeTabId = state.tabs[0].id;
         renderCodeTabs();
      }
   });

   els.dtabs.forEach(btn => {
      btn.addEventListener('click', () => {
         if (btn.hasAttribute('disabled')) return;
         els.dtabs.forEach(b => b.classList.remove('active'));
         els.panels.forEach(p => p.classList.remove('active'));
         btn.classList.add('active');
         document.getElementById(`panel-${btn.dataset.tab}`).classList.add('active');
      });
   });

   els.genBtn.addEventListener('click', () => {
      // Save current tab content
      const activeTextarea = document.getElementById(`code-area-${state.activeTabId}`);
      if (activeTextarea) {
         const tab = state.tabs.find(t => t.id === state.activeTabId);
         if (tab) tab.content = activeTextarea.value;
      }

      const allCode = state.tabs.map(t => t.content).join('\n\n');
      if (!allCode.trim()) {
         showStatus('⚠ Please paste code before generating', '#f59e0b');
         return;
      }

      showStatus(`⏳ Generating diagrams for ${state.language}...`, '#60a5fa');

      try {
         document.getElementById('show-full-box').style.display = 'none';
         if (state.language === 'SQL') {
            const data = parsers.sql.parse(allCode);
            state.diagrams.er = data;
            
            // Desktop Logic: Map ER to Relational
            const relSchema = parsers.sql.mapToRelational(data.erDiagram);
            state.diagrams.relational = relSchema;

            // Re-query canvas elements fresh to avoid stale references after clear
            const erCanvas = document.getElementById('er-canvas');
            const eerdCanvas = document.getElementById('eerd-canvas');
            const relCanvas = document.getElementById('rel-canvas');

            renderers.er.render(erCanvas, data, false);
            renderers.er.render(eerdCanvas, data, true); 
            renderers.relational.render(relCanvas, relSchema);
            
            // Normalized SQL
            els.sqlOutput.value = parsers.sql.generateSQL(relSchema);
            
            // Switch to ER tab
            els.dtabs.forEach(b => b.classList.remove('active'));
            els.panels.forEach(p => p.classList.remove('active'));
            document.querySelector('[data-tab="er"]').classList.add('active');
            document.getElementById('panel-er').classList.add('active');
            
         } else {
            const parser = parsers[state.language.toLowerCase()];
            if (parser) {
               const diagram = parser.parse(allCode);
               state.diagrams.uml = diagram;
               renderers.uml.render(els.umlCanvas, diagram);
               updateSidebarClasses(diagram);
               
               // Generate REAL SOLID analysis report
               els.solidOutput.value = generateSOLIDReport(diagram);
            } else {
                showStatus(`❌ Parser not implemented for ${state.language}`, '#f87171');
                return;
            }
         }
         showStatus('✅ Generation complete!', '#4ade80');
      } catch (err) {
         console.error(err);
         showStatus(`❌ Error: ${err.message}`, '#f87171');
      }
   });

   els.clearBtn.addEventListener('click', () => {
      state.tabs = [{ id: 1, name: 'Main', content: '' }];
      state.activeTabId = 1;
      state.diagrams = { uml: null, er: null };
      renderCodeTabs();
      els.classList.innerHTML = '';
      els.solidOutput.value = '';
      els.sqlOutput.value = '';
      document.getElementById('show-full-box').style.display = 'none';
      document.getElementById('panel-uml').innerHTML = '';
      // Preserve the canvas container divs so cached refs don't go stale
      const erC = document.getElementById('er-canvas');
      const eerdC = document.getElementById('eerd-canvas');
      const relC = document.getElementById('rel-canvas');
      if (erC) erC.innerHTML = '';
      if (eerdC) eerdC.innerHTML = '';
      if (relC) relC.innerHTML = '';
      showStatus('🗑 Cleared all diagrams', '#94a3b8');
   });

   // UML -> Code Navigation
   document.getElementById('uml-to-code-btn').addEventListener('click', () => {
      document.getElementById('uml-code-overlay').style.display = 'flex';
      renderUMLCodeTabs();
   });
   document.getElementById('back-btn').addEventListener('click', () => {
      document.getElementById('uml-code-overlay').style.display = 'none';
   });
   
   // Full Diagram View
   document.getElementById('show-full-btn').addEventListener('click', () => {
       document.getElementById('show-full-box').style.display = 'none';
       document.querySelectorAll('.class-btn').forEach(b => b.classList.remove('active'));
       if(state.diagrams.uml) {
           renderers.uml.render(els.umlCanvas, state.diagrams.uml);
       }
   });
   // Export Menu & PNG Export
   const exportBtn = document.getElementById('export-btn');
   const exportMenu = document.getElementById('export-menu');
   if(exportBtn && exportMenu) {
       exportBtn.addEventListener('click', () => {
          exportMenu.style.display = exportMenu.style.display === 'block' ? 'none' : 'block';
       });
       document.addEventListener('click', (e) => {
          if(!exportBtn.contains(e.target) && !exportMenu.contains(e.target)) {
             exportMenu.style.display = 'none';
          }
       });
   }

   const exportPngBtn = document.getElementById('export-png-btn');
   if(exportPngBtn) {
       exportPngBtn.addEventListener('click', () => {
           if(exportMenu) exportMenu.style.display = 'none';
           const tab = document.querySelector('.dtab.active').getAttribute('data-tab');
           
           if (tab === 'uml') {
               const content = document.getElementById('uml-content-area');
               const boxes = document.getElementById('uml-boxes');
               if (!content || !boxes) return;
               showStatus('Generating PNG... Please wait.', '#fbbf24');
               
               let maxX = 800, maxY = 600;
               Array.from(boxes.children).forEach(b => {
                   let r = parseInt(b.style.left||0) + b.offsetWidth;
                   let d = parseInt(b.style.top||0) + b.offsetHeight;
                   if (r > maxX) maxX = r;
                   if (d > maxY) maxY = d;
               });
               
               const oldW = content.style.width;
               const oldH = content.style.height;
               const oldT = content.style.transform;
               content.style.width = (maxX + 50) + 'px';
               content.style.height = (maxY + 50) + 'px';
               content.style.transform = 'scale(1)';
               
               const restore = () => {
                   content.style.width = oldW;
                   content.style.height = oldH;
                   content.style.transform = oldT;
                   showStatus('Ready', '#94a3b8');
               };
               
               if (!window.html2canvas) {
                   const script = document.createElement('script');
                   script.src = 'https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js';
                   script.onload = () => {
                       window.html2canvas(content, { backgroundColor: '#0f172a', scale: 2 }).then(canvas => {
                           const link = document.createElement('a');
                           link.download = `uml_diagram.png`;
                           link.href = canvas.toDataURL('image/png');
                           link.click();
                           restore();
                       });
                   };
                   document.head.appendChild(script);
               } else {
                   window.html2canvas(content, { backgroundColor: '#0f172a', scale: 2 }).then(canvas => {
                       const link = document.createElement('a');
                       link.download = `uml_diagram.png`;
                       link.href = canvas.toDataURL('image/png');
                       link.click();
                       restore();
                   });
               }
           } else {
                const canvas = document.getElementById(tab + '-canvas');
                if (canvas) {
                    const link = document.createElement('a');
                    link.download = `diagram_${tab}.png`;
                    link.href = canvas.toDataURL();
                    link.click();
                }
            }
        });
    }

    const exportSqlBtn = document.getElementById('export-sql-btn');
    if(exportSqlBtn) {
        exportSqlBtn.addEventListener('click', () => {
            if(exportMenu) exportMenu.style.display = 'none';
            if(!els.sqlOutput.value) {
                showStatus('⚠ No SQL Script generated yet', '#f59e0b');
                return;
            }
            const blob = new Blob([els.sqlOutput.value], { type: 'text/plain' });
            const link = document.createElement('a');
            link.download = 'normalized_schema.sql';
            link.href = URL.createObjectURL(blob);
            link.click();
            showStatus('✅ SQL Script exported!', '#4ade80');
        });
    }

    const exportMappingBtn = document.getElementById('export-mapping-btn');
    if(exportMappingBtn) {
        exportMappingBtn.addEventListener('click', () => {
            if(exportMenu) exportMenu.style.display = 'none';
            if(!state.diagrams.relational) {
                showStatus('⚠ No Relational Mapping generated yet', '#f59e0b');
                return;
            }
            let text = "📋 RELATIONAL MAPPING REPORT\n══════════════════════════════\n\n";
            state.diagrams.relational.tables.forEach(t => {
                text += `Table: ${t.name}\n`;
                t.columns.forEach(c => {
                    text += `  - ${c.name} (${c.type})${c.isPrimaryKey ? ' [PK]' : ''}${c.isForeignKey ? ' [FK -> ' + (c.referencedTable || 'null') + '(' + (c.referencedColumn || 'id') + ')]' : ''}\n`;
                });
                text += "\n";
            });
            const blob = new Blob([text], { type: 'text/plain' });
            const link = document.createElement('a');
            link.download = 'relational_mapping.txt';
            link.href = URL.createObjectURL(blob);
            link.click();
            showStatus('✅ Relational Mapping exported!', '#4ade80');
        });
    }


    // UML -> Code generation logic
    let umlCodeState = {
        tabs: [{ id: 1, name: 'Class 1', classStr: 'class Class1', attrStr: '', methStr: '' }],
        activeTabId: 1,
        nextId: 2
    };

   function renderUMLCodeTabs() {
       const tabsContainer = document.getElementById('uml-class-tabs');
       const inputContainer = document.getElementById('uml-class-input-container');
       if(!tabsContainer || !inputContainer) return;
       
       tabsContainer.innerHTML = '';
       inputContainer.innerHTML = '';

       umlCodeState.tabs.forEach(tab => {
           const btn = document.createElement('button');
           btn.className = `uml-tab ${tab.id === umlCodeState.activeTabId ? 'active' : ''}`;
           btn.style = `padding:8px 15px; background:${tab.id === umlCodeState.activeTabId ? '#1e293b' : 'transparent'}; color:${tab.id === umlCodeState.activeTabId ? '#e2e8f0' : '#64748b'}; border:none; border-bottom:${tab.id === umlCodeState.activeTabId ? '2px solid #6366f1' : '2px solid transparent'}; font-size:13px; cursor:pointer;`;
           btn.textContent = `📄 ${tab.name}`;
           btn.onclick = () => {
               saveActiveUmlCodeTab();
               umlCodeState.activeTabId = tab.id;
               renderUMLCodeTabs();
           };
           tabsContainer.appendChild(btn);

           if(tab.id === umlCodeState.activeTabId) {
               inputContainer.innerHTML = `
                   <div class="form-group" style="margin-bottom:20px;">
                     <label style="color:#60a5fa; font-weight:bold; font-size:13px; margin-bottom:8px; display:block;">1️⃣ Class Name & Stereotype</label>
                     <input type="text" id="uml-class-input" class="form-input" style="font-family:'Consolas', monospace; background:#0f172a; border:1px solid #334155; color:#e2e8f0; padding:10px; width:100%; box-sizing:border-box; border-radius:4px;" placeholder="e.g.  Student    or    <<abstract>> Animal    or    <<interface>> Drawable" value="${tab.classStr}">
                   </div>
                   
                   <div class="form-group" style="margin-bottom:20px;">
                     <label style="color:#4ade80; font-weight:bold; font-size:13px; margin-bottom:8px; display:block;">2️⃣ Attributes / Fields</label>
                     <textarea id="uml-attr-input" class="form-input" rows="6" style="font-family:'Consolas', monospace; background:#0f172a; border:1px solid #334155; color:#e2e8f0; padding:10px; resize:vertical; width:100%; box-sizing:border-box; border-radius:4px;" placeholder="One attribute per line:\\n- name: String\\n+ age: int\\n# count: int {static}\\n- MAX_SIZE: int {static, final}">${tab.attrStr}</textarea>
                   </div>
                   
                   <div class="form-group" style="margin-bottom:20px;">
                     <label style="color:#fbbf24; font-weight:bold; font-size:13px; margin-bottom:8px; display:block;">3️⃣ Methods / Constructors</label>
                     <textarea id="uml-method-input" class="form-input" rows="8" style="font-family:'Consolas', monospace; background:#0f172a; border:1px solid #334155; color:#e2e8f0; padding:10px; resize:vertical; width:100%; box-sizing:border-box; border-radius:4px;" placeholder="One method per line:\\n+ Student(name: String, age: int)\\n+ getName(): String\\n- calculateGPA(): double\\n+ display(): void {static}\\n+ doWork(): void {abstract}">${tab.methStr}</textarea>
                   </div>
               `;
               
               // Auto-generate on input
               const triggerGen = () => {
                   saveActiveUmlCodeTab();
                   document.getElementById('generate-code-btn').click();
               };
               document.getElementById('uml-class-input').addEventListener('input', triggerGen);
               document.getElementById('uml-attr-input').addEventListener('input', triggerGen);
               document.getElementById('uml-method-input').addEventListener('input', triggerGen);
           }
       });
   }

   function saveActiveUmlCodeTab() {
       const activeTab = umlCodeState.tabs.find(t => t.id === umlCodeState.activeTabId);
       if(activeTab) {
           const cInput = document.getElementById('uml-class-input');
           const aInput = document.getElementById('uml-attr-input');
           const mInput = document.getElementById('uml-method-input');
           if(cInput) activeTab.classStr = cInput.value;
           if(aInput) activeTab.attrStr = aInput.value;
           if(mInput) activeTab.methStr = mInput.value;
       }
   }

   const btnAddUmlCls = document.getElementById('add-uml-class-btn');
   if(btnAddUmlCls) btnAddUmlCls.addEventListener('click', () => {
       saveActiveUmlCodeTab();
       const id = umlCodeState.nextId++;
       umlCodeState.tabs.push({ id, name: `Class ${umlCodeState.tabs.length + 1}`, classStr: `class Class${id}`, attrStr: '', methStr: '' });
       umlCodeState.activeTabId = id;
       renderUMLCodeTabs();
   });

   const btnRmUmlCls = document.getElementById('remove-uml-class-btn');
   if(btnRmUmlCls) btnRmUmlCls.addEventListener('click', () => {
       if(umlCodeState.tabs.length > 1) {
           umlCodeState.tabs = umlCodeState.tabs.filter(t => t.id !== umlCodeState.activeTabId);
           umlCodeState.tabs.forEach((t, i) => t.name = `Class ${i + 1}`);
           umlCodeState.activeTabId = umlCodeState.tabs[0].id;
           renderUMLCodeTabs();
       }
   });

    function parseUmlTab(tab) {
        const classInput = tab.classStr.trim() || '';
        const attrInput = tab.attrStr.trim();
        const methInput = tab.methStr.trim();
        
        let stereotype = 'class';
        let className = '';
       let extendsCls = '';
       let implementsCls = '';
       
       let cLine = classInput.replace(/\s+/g, ' ').trim();
       let lowerLine = cLine.toLowerCase();
       
       if (lowerLine.includes('<<abstract>>') || lowerLine.includes('abstract class ')) stereotype = 'abstract';
       else if (lowerLine.includes('<<interface>>') || lowerLine.includes('interface ')) stereotype = 'interface';
       else if (lowerLine.includes('<<enum>>') || lowerLine.includes('enum ')) stereotype = 'enum';
       
       let nameLine = cLine.replace(/<<(.*?)>>/ig, '').trim();
       let nameMatch = nameLine.match(/(?:class|interface|enum)\s+(\w+)/i);
       if (nameMatch) className = nameMatch[1];
       else className = nameLine.split(' ')[0] || '';
       
       let extMatch = cLine.match(/extends\s+(\w+)/);
       if(extMatch) extendsCls = extMatch[1];
       
       let impMatch = cLine.match(/implements\s+([\w,\s]+)/);
       if(impMatch) implementsCls = impMatch[1];
       
       let attributes = [];
       attrInput.split('\n').forEach(line => {
           line = line.trim();
           if(!line) return;
           let access = '';
           if(line.startsWith('+')) access = 'public';
           else if(line.startsWith('-')) access = 'private';
           else if(line.startsWith('#')) access = 'protected';
           else if(line.startsWith('~')) access = 'package';
           
           let isStatic = line.includes('{static}') || line.includes('static ');
           let isFinal = line.includes('{final}') || line.includes('final ');
           
           let cleanLine = line.replace(/^[+#-]\s*/, '').replace(/\{static\}|\{final\}|static |final /g, '').trim();
            let parts = cleanLine.split(':');
            let name = parts[0].trim();
            let type = parts.length > 1 ? parts[1].trim() : '';
           
           attributes.push({ access, type, name, isStatic, isFinal });
       });
       
       let methods = [];
       methInput.split('\n').forEach(line => {
           line = line.trim();
           if(!line) return;
           let access = '';
           if(line.startsWith('+')) access = 'public';
           else if(line.startsWith('-')) access = 'private';
           else if(line.startsWith('#')) access = 'protected';
           else if(line.startsWith('~')) access = 'package';
           
           let isStatic = line.includes('{static}') || line.includes('static ');
           let isAbstract = line.includes('{abstract}') || line.includes('abstract ');
           
           let cleanLine = line.replace(/^[+#-]\s*/, '').replace(/\{static\}|\{abstract\}|static |abstract /g, '').trim();
                      let ret = '';
           let name = 'method';
           let params = '';
           
           let colonIdx = cleanLine.lastIndexOf(':');
           if(colonIdx !== -1) {
               ret = cleanLine.substring(colonIdx + 1).trim();
               cleanLine = cleanLine.substring(0, colonIdx).trim();
           }
           
           let parenIdx = cleanLine.indexOf('(');
           if(parenIdx !== -1) {
               name = cleanLine.substring(0, parenIdx).trim();
               params = cleanLine.substring(parenIdx + 1, cleanLine.lastIndexOf(')')).trim();
           } else {
               name = cleanLine;
           }
           
           methods.push({ access, return: ret, name, params, isStatic, isAbstract });
       });
       
       return { className, stereotype, extendsCls, implementsCls, attributes, methods };
   }

   document.getElementById('generate-code-btn').addEventListener('click', () => {
      saveActiveUmlCodeTab();
      const lang = document.getElementById('gen-language').value;
      
      let fullCode = '';
      let mockDiagram = { classes: [] };
      
      umlCodeState.tabs.forEach(tab => {
          const data = parseUmlTab(tab);
          let code = '';
          
          if (lang === 'Java') {
              code = `public ${data.stereotype === 'abstract' ? 'abstract class' : (data.stereotype === 'interface' ? 'interface' : data.stereotype === 'enum' ? 'enum' : 'class')} ${data.className} `;
              if (data.extendsCls) code += `extends ${data.extendsCls} `;
              if (data.implementsCls) code += `implements ${data.implementsCls} `;
              code += `{\n\n`;
              
              data.attributes.forEach(a => {
                   let typeStr = a.type ? `${a.type} ` : '';
                   code += `    ${a.access} ${a.isStatic ? 'static ' : ''}${a.isFinal ? 'final ' : ''}${typeStr}${a.name};\n`;
               });
              if (data.attributes.length > 0) code += `\n`;
              
              // Default constructor
              let hasDefaultCtor = data.methods.some(m => m.name === data.className && (!m.params || m.params.trim() === ''));
              if (!hasDefaultCtor && data.stereotype !== 'interface' && data.stereotype !== 'enum') {
                  code += `    public ${data.className}() {\n        // Default constructor\n    }\n\n`;
              }
              
              data.methods.forEach(m => {
                  let retStr = m.return ? `${m.return} ` : ''; // constructors have no return
                  code += `    ${m.access} ${m.isStatic ? 'static ' : ''}${m.isAbstract ? 'abstract ' : ''}${retStr}${m.name}(${m.params}) `;
                  if (m.isAbstract || data.stereotype === 'interface') {
                      code += `;\n\n`;
                  } else {
                      if (m.name === data.className) { // Constructor body
                          code += `{\n`;
                          if (m.params) {
                              m.params.split(',').forEach(p => {
                                  let pName = p.trim().split(' ')[1] || p.trim().split(':')[0].trim();
                                  code += `        this.${pName} = ${pName};\n`;
                              });
                          }
                          code += `    }\n\n`;
                      } else {
                          code += `{\n        // TODO: Auto-generated method stub\n        ${m.return && m.return !== 'void' ? 'return null;' : ''}\n    }\n\n`;
                      }
                  }
              });
              
              // Getters and Setters
              if (data.stereotype !== 'interface' && data.stereotype !== 'enum') {
                  data.attributes.forEach(a => {
                      if (a.isStatic && a.isFinal) return; // skip constants
                      let cap = a.name.charAt(0).toUpperCase() + a.name.slice(1);
                      let typeStr = a.type || '';
                      let prefix = typeStr.toLowerCase() === 'boolean' ? 'is' : 'get';
                      code += `    public ${typeStr} ${prefix}${cap}() {\n        return this.${a.name};\n    }\n\n`;
                      if (!a.isFinal) {
                          code += `    public void set${cap}(${typeStr} ${a.name}) {\n        this.${a.name} = ${a.name};\n    }\n\n`;
                      }
                  });
              }
              
              code += `}\n`;
          } else if (lang === 'Python') {
              code = `class ${data.className}`;
              if (data.extendsCls) code += `(${data.extendsCls})`;
              code += `:\n`;
              let hasContent = false;
              data.attributes.forEach(a => {
                  if (a.isStatic) { code += `    ${a.name} = None\n`; hasContent = true; }
              });
              
              code += `\n    def __init__(self):\n`;
              let hasInit = false;
              data.attributes.forEach(a => {
                  if (!a.isStatic) { code += `        self.${a.access === 'private' ? '__' : ''}${a.name} = None\n`; hasInit = true; }
              });
              if(!hasInit) code += `        pass\n`;
              hasContent = true;
                            data.methods.forEach(m => {
                   if (m.name === data.className) return; // skip mapped constructor
                   code += `\n    def ${m.access === 'private' ? '__' : ''}${m.name}(self${m.params ? ', ' + m.params : ''}):\n`;
                   code += `        # TODO: Implement method\n        pass\n`;
               });

               // Python Getters/Setters
               data.attributes.forEach(a => {
                   if (a.isStatic) return;
                   let propName = a.name;
                   let privName = `self.${a.access === 'private' ? '__' : ''}${a.name}`;
                   code += `\n    @property\n    def ${propName}(self):\n        return ${privName}\n`;
                   if (!a.isFinal) {
                       code += `\n    @${propName}.setter\n    def ${propName}(self, value):\n        ${privName} = value\n`;
                   }
               });
          } else if (lang === 'C++') {
              code = `class ${data.className} `;
              if (data.extendsCls) code += `: public ${data.extendsCls} `;
              code += `{\n`;
              
              ['public', 'protected', 'private'].forEach(acc => {
                  let attrs = data.attributes.filter(a => a.access === acc);
                  let meths = data.methods.filter(m => m.access === acc);
                  if(attrs.length > 0 || meths.length > 0) {
                       code += `${acc}:\n`;
                       attrs.forEach(a => {
                           let typeStr = a.type ? `${a.type} ` : '';
                           code += `    ${a.isStatic ? 'static ' : ''}${typeStr}${a.name};\n`;
                       });
                       meths.forEach(m => {
                           let retStr = m.return ? `${m.return} ` : '';
                           code += `    ${m.isAbstract ? 'virtual ' : ''}${retStr}${m.name}(${m.params})`;
                           if(m.isAbstract) code += ` = 0;\n`;
                           else code += `;\n`;
                       });
                       
                       // C++ Getters/Setters within access sections
                       if (acc === 'public') {
                           data.attributes.forEach(a => {
                               if (a.access === 'public') return; // already public
                               let cap = a.name.charAt(0).toUpperCase() + a.name.slice(1);
                               let typeStr = a.type || '';
                               code += `    ${typeStr} get${cap}() const { return ${a.name}; }\n`;
                               if (!a.isFinal) {
                                   code += `    void set${cap}(${typeStr} val) { ${a.name} = val; }\n`;
                               }
                           });
                       }
                   }      });
              code += `};\n`;
          }
          
          fullCode += code + '\n';
          
          mockDiagram.classes.push({
              name: data.className,
              type: data.stereotype.toUpperCase() === 'CLASS' ? 'CLASS' : data.stereotype.toUpperCase(),
              attributes: data.attributes,
              methods: data.methods.map(m => ({ ...m, rawBody: m.name.toLowerCase() }))
          });
      });
      
      document.getElementById('gen-code-output').value = fullCode;
       document.getElementById('gen-solid-output').value = generateSOLIDReport(mockDiagram);
    });

    // Theme Selector for UML-to-Code
    const genThemeSelector = document.getElementById('gen-theme-selector');
    if (genThemeSelector) {
        genThemeSelector.addEventListener('change', (e) => {
            const theme = e.target.value;
            document.body.className = `theme-${theme}`;
            // Sync with main theme selector if it exists
            const mainThemeSelector = document.getElementById('theme-selector');
            if (mainThemeSelector) mainThemeSelector.value = theme;
        });
    }
   
   // Handle Output Tabs
   document.getElementById('tab-boiler').addEventListener('click', () => {
       document.getElementById('tab-boiler').classList.add('active');
       document.getElementById('tab-boiler').style.color = '#818cf8';
       document.getElementById('tab-solid').classList.remove('active');
       document.getElementById('tab-solid').style.color = '#64748b';
       document.getElementById('gen-code-output').style.display = 'block';
       document.getElementById('gen-solid-output').style.display = 'none';
   });
   document.getElementById('tab-solid').addEventListener('click', () => {
       document.getElementById('tab-solid').classList.add('active');
       document.getElementById('tab-solid').style.color = '#4ade80';
       document.getElementById('tab-boiler').classList.remove('active');
       document.getElementById('tab-boiler').style.color = '#64748b';
       document.getElementById('gen-solid-output').style.display = 'block';
       document.getElementById('gen-code-output').style.display = 'none';
   });

   const copyBtn = document.getElementById('copy-code-btn');
   if(copyBtn) {
       copyBtn.addEventListener('click', () => {
           const boilerTab = document.getElementById('tab-boiler');
           const isSolidActive = document.getElementById('tab-solid') && document.getElementById('tab-solid').classList.contains('active');
           const out = isSolidActive ? document.getElementById('gen-solid-output') : document.getElementById('gen-code-output');
           
           if(navigator.clipboard && window.isSecureContext) {
               navigator.clipboard.writeText(out.value);
           } else {
               out.select();
               document.execCommand('copy');
           }
           
           // Show popup
           const orig = copyBtn.innerHTML;
           copyBtn.innerHTML = '✅ Copied to clipboard!';
           copyBtn.style.background = '#10b981'; // green
           setTimeout(() => {
               copyBtn.innerHTML = orig;
               copyBtn.style.background = '#6366f1';
           }, 2000);
           
           // Or an actual alert if preferred
           alert("Copied to clipboard!");
       });
   }

   // Tips Toggle Logic
   const tipsToggle = document.getElementById('uml-tips-toggle');
   const tipsBtn = document.getElementById('tips-btn');
   const tipsContent = document.getElementById('uml-tips-content');
   const tipsIcon = document.getElementById('uml-tips-icon');
   
   function toggleTips() {
       if(tipsContent) {
           if(tipsContent.style.display === 'none') {
               tipsContent.style.display = 'block';
               if(tipsIcon) tipsIcon.textContent = '▲';
               // scroll to bottom to show tips
               const container = document.getElementById('uml-class-input-container');
               if(container) container.parentElement.scrollTop = container.parentElement.scrollHeight;
           } else {
               tipsContent.style.display = 'none';
               if(tipsIcon) tipsIcon.textContent = '▼';
           }
       }
   }
   
   if(tipsToggle) tipsToggle.addEventListener('click', toggleTips);
   if(tipsBtn) tipsBtn.addEventListener('click', toggleTips);

   // --- Helper Functions ---
   function renderCodeTabs() {
      els.tabsHeader.innerHTML = '';
      els.tabsBody.innerHTML = '';
      
      state.tabs.forEach(tab => {
         // Header
         const btn = document.createElement('button');
         btn.className = `ctab ${tab.id === state.activeTabId ? 'active' : ''}`;
         btn.textContent = tab.name;
         btn.onclick = () => {
            // Save current content first
            const currArea = document.getElementById(`code-area-${state.activeTabId}`);
            if (currArea) {
               const currTab = state.tabs.find(t => t.id === state.activeTabId);
               if (currTab) currTab.content = currArea.value;
            }
            state.activeTabId = tab.id;
            renderCodeTabs();
         };
         els.tabsHeader.appendChild(btn);
         
         // Body
         const panel = document.createElement('div');
         panel.className = `code-panel ${tab.id === state.activeTabId ? 'active' : ''}`;
         panel.innerHTML = `<textarea class="code-input" id="code-area-${tab.id}" placeholder="Paste your ${state.language} code here...">${tab.content}</textarea>`;
         els.tabsBody.appendChild(panel);
      });
   }

   function generateSOLIDReport(diagram) {
       if (!diagram || diagram.classes.length === 0) return "No classes found to analyze.";
       
       let report = "🛡️ SOLID PRINCIPLES ANALYSIS REPORT\n════════════════════════════════════════════════════════════════════════\n\n";
       let totalViolations = 0;
       
       diagram.classes.forEach(cls => {
           let classViolations = [];
           
           // SRP
           let hasDb = false, hasLogic = false, hasUI = false;
           let srpLocations = [];
           cls.methods.forEach(m => {
               let ln = m.name.toLowerCase();
               let mb = (m.rawBody || "").toLowerCase();
               if (ln.includes("save") || ln.includes("update") || ln.includes("delete") || ln.includes("db") || ln.includes("sql") ||
                   mb.includes("insert into") || mb.includes("jdbc") || mb.includes("statement.execute") || mb.includes("repository.save")) {
                   hasDb = true; srpLocations.push("Method (DB): " + m.name);
               }
               if (ln.includes("calculate") || ln.includes("process") || ln.includes("compute") ||
                    mb.includes("math.") || (mb.includes("return ") && (mb.includes("*") || mb.includes("/")))) {
                    hasLogic = true; srpLocations.push("Method (Logic): " + m.name);
                }
               if (ln.includes("print") || ln.includes("show") || ln.includes("render") || ln.includes("ui") || ln.includes("button") ||
                   mb.includes("system.out") || mb.includes("jbutton") || mb.includes("alert") || mb.includes("scanner")) {
                   hasUI = true; srpLocations.push("Method (UI): " + m.name);
               }
           });
           srpLocations = [...new Set(srpLocations)];
           if ((hasDb && hasLogic) || (hasDb && hasUI) || (hasLogic && hasUI)) {
               classViolations.push({
                   principle: "Single Responsibility Principle (SRP) / Low Cohesion",
                   locations: srpLocations,
                   definition: "A class must encapsulate only one system functionality or business logic.",
                   detection: "If a class is creating UI components, handling the database, and checking business rules (like calculations) simultaneously, this indicates an SRP violation and Low Cohesion.",
                   rule: "A class should have only one reason to change. For High Cohesion, methods should work together towards a single, unified purpose."
               });
           }
           
           // OCP
           let ocpLocations = [];
           cls.methods.forEach(m => {
               let ln = m.name.toLowerCase();
               if (ln.includes("type") || ln.includes("kind") || ln.includes("switch") || ln.includes("mode")) ocpLocations.push("Method: " + m.name);
           });
           if (ocpLocations.length > 0) {
               classViolations.push({
                   principle: "Open/Closed Principle (OCP)",
                   locations: ocpLocations,
                   definition: "Software entities should be open for extension but closed for modification.",
                   detection: "If you have to repeatedly edit if-else or switch statements in an existing class to handle specific types or modes, OCP is being violated.",
                   rule: "New features should always be 'Added' via Polymorphism (Inheritance/Interfaces), not by 'Changing' existing procedural code."
               });
           }
           
           // LSP
           let lspLocations = [];
           if (!cls.name.toLowerCase().includes("exception")) {
               cls.methods.forEach(m => {
                   if(m.isConstructor) return;
                   let ln = m.name.toLowerCase();
                   if (ln.includes("throw") || ln.includes("exception") || ln.includes("unsupported")) lspLocations.push("Method: " + m.name);
               });
           }
           if (lspLocations.length > 0) {
               classViolations.push({
                   principle: "Liskov Substitution Principle (LSP)",
                   locations: lspLocations,
                   definition: "Objects of a superclass should be replaceable with objects of its subclasses without breaking the application.",
                   detection: "If you override a parent method in a subclass and leave it empty or throw an UnsupportedOperationException, LSP is being violated.",
                   rule: "A subclass must always fulfill the structural and behavioral 'Contract' established by its Parent class."
               });
           }
           
           // ISP
           if (cls.type === 'INTERFACE' && cls.methods.length >= 5) {
               classViolations.push({
                   principle: "Interface Segregation Principle (ISP)",
                   locations: ["Total Methods: " + cls.methods.length + " (Threshold is 4)"],
                   definition: "No client should be forced to depend on methods it does not use.",
                   detection: "If there is a large interface with many methods, but an implementing class only needs a few and is forced to implement the rest as dummy methods, ISP is being violated.",
                   rule: "Large 'fat' interfaces should be split into smaller, more specific interfaces."
               });
           }
           
           // DIP
           let dipLocations = [];
           cls.attributes.forEach(a => {
               let t = a.type.toLowerCase();
               if (t.includes("arraylist") || t.includes("hashmap") || t.includes("mysql") || t.includes("impl")) dipLocations.push("Attribute: " + a.name + " (" + a.type + ")");
           });
           cls.methods.forEach(m => {
               let p = m.params.toLowerCase();
               if (p.includes("arraylist") || p.includes("hashmap") || p.includes("mysql") || p.includes("impl")) dipLocations.push("Method Parameter: " + m.name + "(" + m.params + ")");
           });
           if (dipLocations.length > 0) {
               classViolations.push({
                   principle: "Dependency Inversion Principle (DIP) / Tight Coupling",
                   locations: dipLocations,
                   definition: "High-level modules should not depend on low-level modules. Both should depend on abstractions.",
                   detection: "If your class contains direct usages of concrete class implementations instead of their abstract interfaces, it results in 'Tightly Coupled' code and a DIP violation.",
                   rule: "Classes should always depend on Interfaces/Abstract classes rather than concrete implementations. Loose Coupling is preferred."
               });
           }
           
           if (classViolations.length > 0) {
               report += `🔹 Class: ${cls.name}\n`;
               classViolations.forEach(v => {
                   report += `   ⚠️ ${v.principle} (${v.locations.length} violations):\n`;
                   if (v.locations.length > 0) {
                       report += `      Locations:  ${v.locations.join(", ")}\n`;
                   }
                   report += `      Definition: ${v.definition}\n`;
                   report += `      Detection:  ${v.detection}\n`;
                   report += `      Rule:       ${v.rule}\n\n`;
                   totalViolations += v.locations.length === 0 ? 1 : v.locations.length;
               });
           }
       });
       
       if (totalViolations === 0) {
           report += `✅ Congratulations! No obvious SOLID principle violations detected.\n`;
       } else {
           report += `Total violations found: ${totalViolations}\n`;
       }
       
       return report;
   }

   function updateUIForLanguage() {
      const isSQL = state.language === 'SQL';
      document.querySelector('[data-tab="uml"]').disabled = isSQL;
      document.querySelector('[data-tab="solid"]').disabled = isSQL;
      
      document.querySelector('[data-tab="er"]').disabled = !isSQL;
      document.querySelector('[data-tab="eerd"]').disabled = !isSQL;
      document.querySelector('[data-tab="relational"]').disabled = !isSQL;
      document.querySelector('[data-tab="sql"]').disabled = !isSQL;

      // Auto switch tabs
      els.dtabs.forEach(b => b.classList.remove('active'));
      els.panels.forEach(p => p.classList.remove('active'));
      
      if (isSQL) {
         document.querySelector('[data-tab="er"]').classList.add('active');
         document.getElementById('panel-er').classList.add('active');
      } else {
         document.querySelector('[data-tab="uml"]').classList.add('active');
         document.getElementById('panel-uml').classList.add('active');
      }
   }

   function updateSidebarClasses(diagram) {
      els.classList.innerHTML = '';
      if (!diagram) return;
      diagram.classes.forEach(cls => {
         const btn = document.createElement('button');
         btn.className = 'class-btn';
         let icon = '🔵';
         if (cls.type === 'INTERFACE') icon = '🟣';
         if (cls.type === 'ABSTRACT_CLASS') icon = '🔷';
         if (cls.type === 'ENUM') icon = '🟡';
         if (cls.type === 'RECORD') icon = '🟢';
         
         btn.textContent = `${icon} ${cls.name}`;
         btn.addEventListener('click', () => {
             document.querySelectorAll('.class-btn').forEach(b => b.classList.remove('active'));
             btn.classList.add('active');
             
             document.getElementById('show-full-box').style.display = 'block';
             
             const filteredDiagram = {
                 classes: [cls],
                 relationships: state.diagrams.uml.relationships.filter(r => r.source === cls.name || r.target === cls.name)
             };
             
             const relatedNames = new Set(filteredDiagram.relationships.map(r => r.source === cls.name ? r.target : r.source));
             state.diagrams.uml.classes.forEach(c => {
                 if (relatedNames.has(c.name) && c.name !== cls.name) {
                     filteredDiagram.classes.push(c);
                 }
             });
             
             renderers.uml.render(els.umlCanvas, filteredDiagram);
         });
         els.classList.appendChild(btn);
      });
   }

   function showStatus(msg, color) {
      els.status.textContent = msg;
      els.status.style.color = color;
   }
   
   // Folder scanning setup
   const scanBtn = document.getElementById('scan-folder-btn');
   const scanInput = document.getElementById('scan-file-input');
   if (scanBtn && scanInput) {
      scanBtn.addEventListener('click', () => {
         scanInput.click();
      });
      scanInput.addEventListener('change', (e) => {
         const files = Array.from(e.target.files);
         if (files.length === 0) return;
         
         // Clear existing except first if empty
         if (state.tabs.length === 1 && !state.tabs[0].content) {
            state.tabs = [];
         }
         
         let loaded = 0;
         files.forEach(file => {
            // Only process known extensions
            if (!file.name.match(/\.(java|py|cpp|h|hpp|sql)$/)) {
               loaded++;
               if (loaded === files.length) {
                   if(state.tabs.length > 0) state.activeTabId = state.tabs[0].id;
                   renderCodeTabs();
               }
               return;
            }
            const reader = new FileReader();
            reader.onload = (ev) => {
               const content = ev.target.result;
               const id = Date.now() + Math.random();
               state.tabs.push({ id, name: file.name, content });
               loaded++;
               if (loaded === files.length) {
                   if(state.tabs.length > 0) state.activeTabId = state.tabs[0].id;
                   renderCodeTabs();
               }
            };
            reader.readAsText(file);
         });
      });
   }
   
   // PWA Installation Logic
   let deferredPrompt;
   const installBtn = document.getElementById('install-pwa-btn');

   window.addEventListener('beforeinstallprompt', (e) => {
       e.preventDefault();
       deferredPrompt = e;
       if (installBtn) installBtn.style.display = 'block';
   });

   if (installBtn) {
       installBtn.addEventListener('click', async () => {
           if (!deferredPrompt) return;
           deferredPrompt.prompt();
           const { outcome } = await deferredPrompt.userChoice;
           if (outcome === 'accepted') {
               installBtn.style.display = 'none';
           }
           deferredPrompt = null;
       });
   }

   // Run Initial Setup
   updateUIForLanguage();
});
