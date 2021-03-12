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
        <li @click="(enableDelete) ? deleteNode() : false" class="context-action delete-nodes" :class="{'disabled':!enableDelete}">Delete</li>
    </div>
</template>
<script>
  import { DELETE_SCHEMA_CONCEPT } from '@/components/shared/StoresActions';
  import { createNamespacedHelpers } from 'vuex';


  export default {
    name: 'ContextMenu',
    props: [],
    beforeCreate() {
      const { mapGetters, mapMutations, mapActions } = createNamespacedHelpers('schema-design');

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['contextMenu', 'selectedNodes']),
      };

      // methods
      this.$options.methods = {
        ...(this.$options.methods || {}),
        ...mapMutations(['setContextMenu']),
        ...mapActions([DELETE_SCHEMA_CONCEPT]),
      };
    },
    computed: {
      enableDelete() {
        return (this.selectedNodes);
      },
    },
    watch: {
      contextMenu(contextMenu) {
        this.$refs.contextMenu.style.left = `${contextMenu.x}px`;
        this.$refs.contextMenu.style.top = `${contextMenu.y}px`;
      },
    },
    methods: {
      async deleteNode() {
        this.setContextMenu({ show: false, x: null, y: null });

        if (this.selectedNodes.length > 1) {
          this.$notifyError('Cannot delete multiple schema concepts');
        } else {
          const label = this.selectedNodes[0].label;

          this[DELETE_SCHEMA_CONCEPT](this.selectedNodes[0])
            .then(() => {
              this.$notifyInfo(`Schema concept, ${label}, has been deleted`);
            })
            .catch((e) => { this.$notifyError(e); });
        }
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
    }

    .context-action:not(.disabled):hover{
        opacity: 1;
        background-color: #404040;
    }
</style>


