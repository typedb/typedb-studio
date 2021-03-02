<!--
 Copyright (C) 2021 Grakn Labs

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
    <div class="settings-tab-content">
        <div class="tab-header noselect">Settings</div>
        <query-settings-panel></query-settings-panel>
        <concept-display-panel :tabId="tabId"></concept-display-panel>
    </div>
</template>
<script>
  import QuerySettingsPanel from './SettingsTab/QuerySettingsPanel.vue';
  import ConceptDisplayPanel from './SettingsTab/DisplaySettingsPanel';

  export default {
    name: 'SettingsTab',
    props: ['tabId'],
    components: { QuerySettingsPanel, ConceptDisplayPanel },
  };
</script>

<style scoped>
    .settings-tab-content {
        width: 214.14px;
    }
</style>
