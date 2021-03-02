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
    <div class="panel-container">
        <div @click="toggleContent" class="panel-header">
            <vue-icon :icon="(showConceptInfoContent) ?  'chevron-down' : 'chevron-right'" iconSize="14" className="vue-icon"></vue-icon>
            <h1>Identity</h1>
        </div>
        <div v-show="showConceptInfoContent">
            <div class="content noselect" v-if="!currentDatabase">
                Please select a database
            </div>
            <div class="content noselect" v-else-if="!(selectedNodes && selectedNodes.length === 1)">
                Please select a node
            </div>
            <div class="content" v-else>
                <div class="content-item" v-if="conceptInfo.iid">
                    <h1 class="label">IID:</h1>
                    <div class="value">{{conceptInfo.iid}}</div>
                </div>
                <div class="content-item" v-if="conceptInfo.typeLabel">
                    <h1 class="label" v-if="conceptInfo.iid">TYPE:</h1>
                    <h1 class="label" v-else>LABEL:</h1>
                    <div class="value">{{conceptInfo.typeLabel}}</div>
                </div>
                <div class="content-item">
                    <h1 class="label">BASE TYPE:</h1>
                    <div class="value">{{conceptInfo.baseType}}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  export default {
    name: 'IdentityPanel',
    props: [],
    data() {
      return {
        showConceptInfoContent: true,
      };
    },
    beforeCreate() {
      const { mapGetters } = createNamespacedHelpers('schema-design');

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['currentDatabase', 'selectedNodes']),
      };
    },
    computed: {
      conceptInfo() {
        if (!this.selectedNodes) return {};
        const node = this.selectedNodes[0];
        if (node.baseType.includes('TYPE')) {
          return {
            typeLabel: node.typeLabel,
            baseType: node.baseType,
          };
        }
        return {
          iid: node.iid,
          typeLabel: node.typeLabel,
          baseType: (node.isInferred === true) ? `INFERRED_${node.baseType}` : node.baseType,
        };
      },
    },
    watch: {
      selectedNodes(nodes) {
        if (nodes && nodes.length === 1) this.showConceptInfoContent = true;
      },
    },
    methods: {
      toggleContent() {
        this.showConceptInfoContent = !this.showConceptInfoContent;
      },
    },
  };
</script>

<style scoped>

    .content {
        padding: var(--container-padding);
        display: flex;
        flex-direction: column;
        max-height: 90px;
        justify-content: center;
        border-bottom: var(--container-darkest-border);
    }

    .content-item {
        padding: var(--container-padding);
        display: flex;
        flex-direction: row;
    }

    .label {
        margin-right: 20px;
        width: 66px;
    }

</style>
