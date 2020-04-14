
//
//
// https://ace.c9.io/api/editor.html

// var ace = require('brace');
// https://github.com/ajaxorg/ace/blob/master/demo/webpack/demo.js

// import ace from 'ace-builds'
// import language_tools from 'ace-builds'
// import ext-themelist from 'ace-builds'
// import ace from 'ace-builds'
// import ace from 'ace-builds'
// import ace from 'ace-builds'

// require("ace-builds/ext/language_tools"); //language extension prerequsite...


// https://github.com/ajaxorg/ace-builds/issues/129
///////////////////////////////
// EXAMPLE 1
////////////////////////////////////
// import 'ace-builds/src-min-noconflict/ace' // Load Ace Editor

// Import initial theme and mode so we don't have to wait for 2 extra HTTP requests
// import 'ace-builds/src-min-noconflict/theme-chrome'
// import 'ace-builds/src-min-noconflict/mode-javascript'

///////////////////////////////
// EXAMPLE 2
////////////////////////////////////
var ace = require("ace-builds")
require("ace-builds/webpack-resolver");
require("ace-builds/src-noconflict/mode-json");
require("ace-builds/src-noconflict/ext-language_tools");


// require("ace/mode/html");
// require("ace/mode/json");
// require("ace/mode/javascript");
// require("ace/mode/kotlin"); //language
// require("ace/mode/less");
// require("ace/theme/monokai");
// require("ace/snippets/kotlin"); //snippet
// require('ace/ext/emmet');

export default {
    render: function (h) {
        var height = this.height ? this.px(this.height) : '100%'
        var width = this.width ? this.px(this.width) : '100%'
        return h('div',{
            attrs: {
                style: "height: " + height  + '; width: ' + width, 
                class: "editor"
            }
        })
    },
    props:{
        view: String,
        value:String,
        lang:true,
        theme:String,
        height:true,
        width:true,
        options:Object
    },
    data: function () {
        return {
            editor:null,
            lock: false 
        }
    },
    methods: {
        px:function (n) {
            if( /^\d*$/.test(n) ){
                return n+"px";
            }
            return n;
        }
    },
    watch:{
        view: function(val) {
            // console.log(":vie... watch")
            this.editor.session.setValue(val,1);

        },
        value: function () {
            // console.log("ace x value change")
            // this.lock = true
            // if(va)
            // this.editor.session.setValue(val,1);
        },
        theme:function (newTheme) {
            this.editor.setTheme('ace/theme/'+newTheme);
        },
        lang:function (newLang) {
            this.editor.getSession().setMode(typeof newLang === 'string' ? ( 'ace/mode/' + newLang ) : newLang);
        },
        options:function(newOption){
            this.editor.setOptions(newOption);
        },
        height:function(){
            this.$nextTick(function(){
                this.editor.resize()
            })
        },
        width:function(){
            this.$nextTick(function(){
                this.editor.resize()
            })
        }
    },
    beforeDestroy: function() {
        this.editor.destroy();
        this.editor.container.remove();
    },
    mounted: function () {
        console.log("mounted xeditor")
        const fontSize = 15
        console.log(`fontSize=${fontSize}`)


        

        var vm = this;
        var lang = this.lang||'kotlin'; // text
        var theme = this.theme||'monokai'; //chrome

        var editor = vm.editor = ace.edit(this.$el);
        editor.$blockScrolling = Infinity;

        this.$emit('init',editor);
        

        editor.setFontSize(fontSize) 

        // ace.edit(document.getElementById('test'), {
            // useWorker: false
        // });


        // editor.setOptions({
            // maxLines: Infinity
        // });

        editor.commands.addCommands([{
            name: "showSettingsMenu",
            bindKey: {win: "Ctrl-q", mac: "Ctrl-q"},
            exec: function(editor) {
                editor.showSettingsMenu();
            },
            readOnly: true
        }]);

        //editor.setOption("enableEmmet", true);

        // editor.showSettingsMenu();
        // editor.setKeyboardHandler('vim')
        // const keyboardHandler = editor.getKeyboardHandler()
        // console.log(keyboardHandler)
        // const keyboardHandler = editor.setKeyboardHandler()
        // console.log(`ace keyboard handler`)

        editor.getSession().setUseWorker(false)
        editor.getSession().setMode(typeof lang === 'string' ? ( 'ace/mode/' + lang ) : lang);
        editor.setTheme('ace/theme/'+theme);
        if(this.value) editor.setValue(this.value,1);
        if(this.view) editor.session.setValue(this.view,1)
        // this.contentBackup = this.value;

        editor.on('change', () => {
            // if(this.lock === true ) {
                // this.lock = false
                // return
            // }
            const content = editor.getValue();
            vm.$emit('input',content);
        });



        if(vm.options)
            editor.setOptions(vm.options);
    }
}