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
    <div class="panel-container">
        <div @click="toggleContent" class="panel-header">
            <vue-icon :icon="(showRolesPanel) ?  'chevron-down' : 'chevron-right'" iconSize="14" className="vue-icon"></vue-icon>
            <h1>Plays</h1>
        </div>
        <div v-show="showRolesPanel">
            <div class="content noselect" v-if="msg">
                {{msg}}
            </div>
            <div class="content" v-else>

                <div v-for="(value, index) in roles" :key="index">
                    <div class="content-item">
                        <div class="label">{{value}}</div>
                        <div class="btn right-bar-btn reset-setting-btn" @click="removeRoleType(value, index)"><vue-icon icon="trash" className="vue-icon" iconSize="12"></vue-icon></div>
                    </div>
                </div>

                <div class="add-new-role">
                  <div class="row noselect">
                    <div @click="showNewRolesPanel = !showNewRolesPanel" class="has-header">
                      <vue-icon :icon="(showNewRolesPanel) ?  'chevron-down' : 'chevron-right'" iconSize="14" className="vue-icon"></vue-icon>
                      Add Role Types
                      </div>
                  </div>

                  <div class="row-2 noselect" v-if="showNewRolesPanel">
                    <div class="has">
                      <ul class="role-type-list" v-if="newRoles.length">
                        <li :class="(toggledRoleTypes.includes(roleType)) ? 'role-btn toggle-role-btn' : 'role-btn'" @click="toggleRoleType(roleType)" v-for="roleType in newRoles" :key=roleType>
                            {{roleType}}
                        </li>
                      </ul>
                      <div v-else class="no-types">There are no additional role types defined</div>

                      <div class="btn-row">
                        <button class="btn small-btn" @click="clearRoleTypes">Clear</button>
                        <button class="btn small-btn" @click="addRoleTypes">Add</button>
                      </div>
                    </div>
                  </div>

                </div>
            </div>
        </div>
    </div>
</template>

<script>
  import { ADD_PLAYS, REFRESH_SELECTED_NODE, DELETE_PLAYS } from '@/components/shared/StoresActions';
  import { createNamespacedHelpers } from 'vuex';
  
  export default {
    name: 'PlaysPanel',
    props: [],
    data() {
      return {
        showRolesPanel: true,
        roles: null,
        showNewRolesPanel: false,
        toggledRoleTypes: [],
      };
    },
    mounted() {
      this.loadRoles(this.selectedNodes);
    },
    beforeCreate() {
      const { mapGetters, mapActions } = createNamespacedHelpers('schema-design');

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['selectedNodes', 'currentDatabase', 'metaTypeInstances']),
      };

      // methods
      this.$options.methods = {
        ...(this.$options.methods || {}),
        ...mapActions([ADD_PLAYS, REFRESH_SELECTED_NODE, DELETE_PLAYS]),
      };
    },
    computed: {
      msg() {
        if (!this.currentDatabase) return 'Please select a database';
        else if (!this.selectedNodes || this.selectedNodes.length > 1) return 'Please select a node';
        else if (!this.roles) return 'Roles are being loaded';
        return null;
      },
      newRoles() {
        return (this.roles) ? this.metaTypeInstances.roles.filter(role => !this.roles.map(rol => rol).includes(role)) : [];
      },
    },
    watch: {
      selectedNodes(nodes) {
        this.loadRoles(nodes);
      },
    },
    methods: {
      loadRoles(nodes) {
        // If no node selected: close panel and return
        if (!nodes || nodes.length > 1) return;

        const roles = nodes[0].roles;

        this.roles = Object.values(roles).sort((a, b) => ((a > b) ? 1 : -1));

        this.showRolesPanel = true;
      },
      toggleContent() {
        this.showRolesPanel = !this.showRolesPanel;
      },
      toggleRoleType(type) {
        const index = this.toggledRoleTypes.indexOf(type);
        if (index > -1) {
          this.toggledRoleTypes.splice(index, 1);
        } else {
          this.toggledRoleTypes.push(type);
        }
      },
      clearRoleTypes() {
        this.toggledRoleTypes = [];
      },
      addRoleTypes() {
        this[ADD_PLAYS]({ schemaLabel: this.selectedNodes[0].label, roleTypes: this.toggledRoleTypes })
          .then(() => {
            this[REFRESH_SELECTED_NODE]();
            this.showNewRolesPanel = false;
            this.toggledRoleTypes = [];
          })
          .catch((e) => {
            this.$notifyError(e.message);
          });
      },
      removeRoleType(role, index) {
        this[DELETE_PLAYS]({ schemaLabel: this.selectedNodes[0].label, roleLabel: role, index })
          .then(() => {
            this[REFRESH_SELECTED_NODE]();
          })
          .catch((e) => {
            this.$notifyError(e.message);
          });
      },
    },
  };
</script>

<style scoped>

  .add-new-role {
    padding: var(--container-padding);
  }

  .no-types {
    background-color: var(--gray-1);
    padding: var(--container-padding);
    border: var(--container-darkest-border);
    border-top: 0px;
  }


  .btn-row {
    padding-top: var(--container-padding);
    display: flex;
    justify-content: space-between;
  }


  .content {
      padding: var(--container-padding);
      border-bottom: var(--container-darkest-border);
      display: flex;
      flex-direction: column;
      max-height: 500px;
      overflow: auto;
  }

  .content::-webkit-scrollbar {
      width: 2px;
  }

  .content::-webkit-scrollbar-thumb {
      background: var(--green-4);
  }

  .content-item {
      padding: var(--container-padding);
      display: flex;
      flex-direction: row;
      justify-content: space-between;
      align-items: center;
      height: 20px;
  }

  .label {
      margin-right: 20px;
      width: 66px;
  }

  .has {
    width: 100%;
  }

  .row {
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    white-space: nowrap;
  }

  .row-2 {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
  }

  .has-header {
    width: 100%;
    background-color: var(--gray-1);
    border: var(--container-darkest-border);
    height: 22px;
    display: flex;
    align-items: center;
    cursor: pointer;
  }


  .role-type-list {
    border: var(--container-darkest-border);
    background-color: var(--gray-1);
    width: 100%;
    max-height: 140px;
    overflow: auto;
  }

  .role-type-list::-webkit-scrollbar {
    width: 2px;
  }

  .role-type-list::-webkit-scrollbar-thumb {
    background: var(--green-4);
  }

  /*dynamic*/
  .role-btn {
    align-items: center;
    padding: 2px;
    cursor: pointer;
    white-space: normal;
    word-wrap: break-word;
  }

  /*dynamic*/
  .role-btn:hover {
    background-color: var(--purple-4);
  }

  /*dynamic*/
  .toggle-role-btn {
    background-color: var(--purple-3);
  }

</style>
