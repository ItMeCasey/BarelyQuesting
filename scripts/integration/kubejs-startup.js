global.BarelyQuesting.chapter('kubejs_validation', 'kubejs integration', 'minecraft:diamond');
global.BarelyQuesting.registerTask('kubejs_ready', ctx => ctx.target());
global.BarelyQuesting.quest('kubejs_validation', JSON.stringify({id: 'kubejs_quest', title: 'kubejs callback works', tasks: [{id: 'ready', type: 'scripted', name: 'kubejs_ready', count: 1}]}));
