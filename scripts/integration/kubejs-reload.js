// KubeJS server script for the reload check: clears the old listeners, then registers one that prints a marker.
global.BarelyQuesting.clearListeners()
global.BarelyQuesting.onQuestCompleted(e => {
    console.info('BQ-LISTENER fired for ' + e.questId())
})
