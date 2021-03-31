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
    <div v-show="contextMenu.show" ref="contextMenu" id="context-menu" class="z-depth-2">
        <li @click="(enableDelete) ? deleteNode() : false" class="context-action delete-nodes" :class="{'disabled':!enableDelete}">Hide</li>
        <li @click="(enableExplain) ? explainNode() : false" class="context-action explain-node" :class="{'disabled':!enableExplain}">Explain</li>
<!--        <li @click="(enableShortestPath) ? computeShortestPath() : false" class="context-action compute-shortest-path" :class="{'disabled':!enableShortestPath}">Shortest Path</li>-->
    </div>
</template>
<script>
  import { RUN_CURRENT_QUERY, EXPLAIN_CONCEPT, DELETE_SELECTED_NODES } from '@/components/shared/StoresActions';
  import { createNamespacedHelpers } from 'vuex';


  export default {
    name: 'ContextMenu',
    props: ['tabId'],
    beforeCreate() {
      const { mapGetters, mapMutations, mapActions } = createNamespacedHelpers(`tab-${this.$options.propsData.tabId}`);

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['currentDatabase', 'contextMenu', 'selectedNodes']),
      };

      // methods
      this.$options.methods = {
        ...(this.$options.methods || {}),
        ...mapMutations(['setCurrentQuery', 'setContextMenu']),
        ...mapActions([RUN_CURRENT_QUERY, DELETE_SELECTED_NODES, EXPLAIN_CONCEPT]),
      };
    },
    computed: {
      enableDelete() {
        return (this.selectedNodes);
      },
      enableExplain() {
        return (this.selectedNodes && this.selectedNodes[0].explainable && !this.selectedNodes[0].explanationExhausted);
      },
      enableShortestPath() {
        return (this.selectedNodes && this.selectedNodes.length === 2);
      },
    },
    watch: {
      contextMenu(contextMenu) {
        this.$refs.contextMenu.style.left = `${contextMenu.x}px`;
        this.$refs.contextMenu.style.top = `${contextMenu.y}px`;
      },
    },
    methods: {
      deleteNode() {
        this.setContextMenu({ show: false, x: null, y: null });
        this[DELETE_SELECTED_NODES]().catch((err) => { this.$notifyError(err, 'Delete nodes'); });
      },
      explainNode() {
        this.setContextMenu({ show: false, x: null, y: null });
        this[EXPLAIN_CONCEPT]().catch((err) => { this.$notifyError(err, 'Explain Concept'); });
      },
      computeShortestPath() {
        this.setContextMenu({ show: false, x: null, y: null });
        this.setCurrentQuery(`compute path from ${this.selectedNodes[0].id}, to ${this.selectedNodes[1].id};`);
        this[RUN_CURRENT_QUERY]().catch((err) => { this.$notifyError(err, 'Run Query'); });
      },
    },
  };
</script>
<style>
    #context-menu{
        position: absolute;
        background-color: #282828;
        padding: 8px 0px;
        z-index: 10;
        min-width: 100px;
        border: var(--container-darkest-border);
    }

    .context-action.disabled{
        opacity: 0.2;
        cursor: default;
    }

    .context-action{
        padding:8px 10px;
        cursor: pointer;
        opacity: 0.8;
        list-style-type: none;
        user-select: none;
        -moz-user-select: none;
        -webkit-user-select: none;
        -ms-user-select: none;
    }

    .context-action:not(.disabled):hover{
        opacity: 1;
        background-color: #404040;
    }
</style>


