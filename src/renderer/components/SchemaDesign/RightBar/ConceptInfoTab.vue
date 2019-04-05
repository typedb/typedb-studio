<template>
    <div class="node-tab-content">
        <div class="tab-header noselect">Concept Info</div>
        <identity-panel></identity-panel>
        <attributes-panel></attributes-panel>
        <plays-panel></plays-panel>
    </div>
</template>
<script>
  import IdentityPanel from './ConceptInfoTab/IdentityPanel';
  import AttributesPanel from './ConceptInfoTab/AttributesPanel';
  import PlaysPanel from './ConceptInfoTab/PlaysPanel';


  export default {
    name: 'ConceptInfoTab',
    components: { IdentityPanel, AttributesPanel, PlaysPanel },
  };
</script>
<style scoped>
    .node-tab-content {
        width: 201px;
    }
</style>
