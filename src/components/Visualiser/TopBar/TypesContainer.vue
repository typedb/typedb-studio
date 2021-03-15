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
        <div class="types-container z-depth-3 noselect">
            <div class="column">
                <vue-tabs class="tabs" :tabs="tabs" v-on:tab-selected="toggleTab"></vue-tabs>
                <div class="row">
                    <div class="tab-panel" v-for="k in Object.keys(metaTypeInstances)" :key="k">
                        <div class="tab-list" v-show="currentTab===k">
                            <div v-for="i in metaTypeInstances[k]" :key="i">
                                <button @click="typeSelected(i)" class="btn select-type-btn">{{i}}</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="editor-tab">
                <div @click="$emit('close-types-panel')"><vue-icon icon="cross" iconSize="12" className="tab-icon"></vue-icon></div>
            </div>
        </div>
</template>

<style scoped>

    .editor-tab {
        max-height: 125px;
        width: 13px;
        flex-direction: column;
        display: flex;
        position: relative;
        float: right;
        border-left: var(--container-light-border);
    }

    .tab-list {
        overflow: auto;
        height: 72px;
        display: flex;
        flex-wrap: wrap;
        flex-direction: row;
    }

    .types-container{
        background-color: var(--gray-2);
        border: var(--container-darkest-border);
        width: 100%;
        margin-top: 10px;
        max-height: 125px;
        position: relative;
        display: flex;
        flex-direction: row;
    }

    .column {
        display: flex;
        flex-direction: column;
        width: 100%;
        overflow-y: auto;
    }

    .column::-webkit-scrollbar {
        width: 1px;
    }

    .column::-webkit-scrollbar-thumb {
        background: var(--green-4);
    }

    .row {
        margin: var(--element-margin);
    }
</style>

<script>
  import { createNamespacedHelpers } from 'vuex';

  export default {
    name: 'TypesContainer',
    data() {
      return {
        tabs: ['entities', 'attributes', 'relations'],
        currentTab: 'entities',
      };
    },
    props: ['tabId'],
    beforeCreate() {
      const { mapGetters, mapMutations } = createNamespacedHelpers(`tab-${this.$options.parent.$options.propsData.tabId}`);

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['metaTypeInstances']),
      };

      // methods
      this.$options.methods = {
        ...(this.$options.methods || {}),
        ...mapMutations(['setCurrentQuery']),
      };
    },
    methods: {
      toggleTab(tab) {
        this.currentTab = tab;
      },
      typeSelected(type) {
        this.setCurrentQuery(`match $x isa ${type};`);
      },
    },
  };
</script>
