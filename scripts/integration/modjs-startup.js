BarelyQuesting.chapter('modjs_validation', 'modjs integration', 'minecraft:diamond');
BarelyQuesting.registerTask('modjs_ready', ctx => ctx.target());
BarelyQuesting.quest('modjs_validation', JSON.stringify({id: 'modjs_quest', title: 'modjs callback works', tasks: [{id: 'ready', type: 'scripted', name: 'modjs_ready', count: 1}]}));
