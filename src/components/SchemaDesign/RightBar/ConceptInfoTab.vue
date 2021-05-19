<!--
 Copyright (C) 2021 Vaticle

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as
 published by the Free Software Foundation, either version 3 of the
 License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <https://www.gnu.org/licenses/>.
-->

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
