<template>
    <div class="node-tab-content">
        <div class="tab-header noselect">Concept Info</div>
        <identity-panel :tabId="tabId"></identity-panel>
        <attributes-panel :tabId="tabId"></attributes-panel>
        <relations-panel :tabId="tabId"></relations-panel>
    </div>
</template>
<script>
  import IdentityPanel from './ConceptInfoTab/IdentityPanel';
  import AttributesPanel from './ConceptInfoTab/AttributesPanel';
  import RelationsPanel from './ConceptInfoTab/RelationsPanel';


  export default {
    name: 'ConceptInfoTab',
    props: ['tabId'],
    components: { IdentityPanel, AttributesPanel, RelationsPanel },
  };
</script>
<style scoped>
    .node-tab-content {
        width: 214.14px;
    }
</style>
