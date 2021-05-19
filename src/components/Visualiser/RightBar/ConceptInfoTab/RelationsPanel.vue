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
            <vue-icon :icon="(showRelationsPanel) ?  'chevron-down' : 'chevron-right'" iconSize="14" className="vue-icon"></vue-icon>
            <h1>Relations</h1>
        </div>

        <div v-show="showRelationsPanel">

            <div class="content noselect" v-if="!currentDatabase">
                Please select a database
            </div>
            <div class="content noselect" v-else-if="(!selectedNodes || selectedNodes.length > 1)">
                Please select a node
            </div>

            <div class="content" v-else>

                <div v-if="loading">
                    Loading...
                </div>
                <div v-else-if="!currentRole">
                    This concept does not take part in any relations
                </div>
                <div v-else class="row plays-row">
                    <div class="label">
                        Plays
                    </div>
                    <div class="value">
                        <div v-bind:class="(showRolesList) ? 'btn role-btn role-list-shown' : 'btn role-btn'" @click="toggleRoleList"><div class="role-btn-text" >{{currentRole | truncate}}</div><vue-icon class="role-btn-caret" className="vue-icon" icon="caret-down"></vue-icon></div>
                    </div>
                </div>

                <div v-if="relations" class="panel-list-item">
                    <div class="role-list" v-show="showRolesList">
                        <ul v-for="role in Array.from(relations.keys())" :key="role">
                            <li class="role-item" @click="selectRole(role)" v-bind:class="[(role === currentRole) ? 'role-item-selected' : '']">{{role}}</li>
                        </ul>
                    </div>
                </div>

                <div v-if="showRolePlayers && relations.get(currentRole)">
                    <div class="column">
                        <div class="row content-item">
                            <div class="label">
                                In
                            </div>
                            <div class="value relation-item" v-if="relations.get(currentRole)">
                                {{relations.get(currentRole).relation}}
                            </div>
                        </div>

                        <div class="row content-item">
                            <div class="label">
                                Where
                            </div>
                            <div class="value">
                            </div>
                        </div>

                        <div v-if="relations.get(currentRole)">
                            <div class="roleplayers-list content-item" v-for="(rp, index) in relations.get(currentRole).otherRolePlayers" :key="index">
                                <div class="label role-label">
                                    {{rp.role}}
                                </div>
                                <div class="value player-value">
                                    {{rp.player}}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </div>

    </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  export default {
    name: 'RelationsPanel',
    props: ['tabId'],
    data() {
      return {
        showRelationsPanel: true,
        showRolesList: false,
        loading: false,
        currentRole: undefined,
        relations: undefined,
        showRolePlayers: false,
      };
    },
    beforeCreate() {
      const { mapGetters } = createNamespacedHelpers(`tab-${this.$options.propsData.tabId}`);

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['selectedNodes', 'currentDatabase']),
      };

      // methods
      this.$options.methods = {
        ...(this.$options.methods || {}),
      };
    },
    watch: {
      async selectedNodes() {
        this.showRolePlayers = false;
        // Initialise new relation map whenever a node is selected
        this.relations = new Map();
        this.loading = true;
        this.currentRole = await this.loadRolesAndRelations();
        this.loading = false;
      },
      async currentRole() {
        this.showRolePlayers = false;
        this.loading = true;
        // For relation of a role, compute all other role players
        await this.loadOtherRolePlayers();
        this.showRolePlayers = true;
        this.loading = false;
      },
    },
    filters: {
      truncate(cr) {
        if (!cr) return null;
        cr = cr.split(':')[1];
        if (cr.length > 13) return `${cr.substring(0, 13)}...`;
        return cr;
      },
    },
    methods: {
      toggleContent() {
        this.showRelationsPanel = !this.showRelationsPanel;
      },
      toggleRoleList() {
        this.showRolesList = !this.showRolesList;
      },
      selectRole(role) {
        this.showRolesList = false;
        this.currentRole = role;
      },
      async loadRolesAndRelations() {
        if (this.selectedNodes && this.selectedNodes.length) {
          const node = this.selectedNodes[0];
          const tx = global.typeDBTx[this.$store.getters.activeTab];
          let roles;
          if (node.iid) {
              const thing = await tx.concepts().getThing(node.iid);
              roles = await thing.asRemote(tx).getPlaying().collect();
          } else if (node.typeLabel) {
              const thingType = await tx.concepts().getThingType(node.typeLabel);
              roles = await thingType.asRemote(tx).getPlays().collect();
          } else {
              throw "Node must have either an IID or a Label";
          }
          // Map roles to their respective relations and an empty array of other role players in that relation
          // Role => { relation, otherRolePlayers = [] }
          for (const role of roles) {
              const roleLabel = role.getLabel().scopedName();
              if (!(roleLabel in this.relations)) {
                  this.relations.set(roleLabel, new Map());
                  (await role.asRemote(tx).getRelationTypes().collect()).forEach((x) => {
                      this.relations.set(roleLabel, { relation: x.getLabel().name(), otherRolePlayers: [] });
                  });
              }
          }
          return this.relations.keys().next().value;
        }
      },
      async loadOtherRolePlayers() {
        // If roleplayers have not already been computed
        if (this.currentRole && this.selectedNodes && this.selectedNodes.length) {
          if (this.relations.get(this.currentRole) && this.relations.get(this.currentRole).otherRolePlayers.length) return;
          const node = this.selectedNodes[0];
          const tx = global.typeDBTx[this.$store.getters.activeTab];
          let concept;
          let roles;

          if (node.iid) {
            concept = await tx.concepts().getThing(node.iid);
            roles = await concept.asRemote(tx).getPlaying().collect();
          } else if (node.typeLabel) {
            concept = await tx.concepts().getThingType(node.typeLabel);
            roles = await concept.asRemote(tx).getPlays().collect();
          } else {
            throw "Node must have either an IID or a Label";
          }

          // Get role concept of selected current role
          const role = roles.find(r => r.getLabel().scopedName() === this.currentRole);

          // For every relation, map relations to their respective rolePlayer and the role it plays
          if (node.iid) {
              // Get relation concepts of current role
              let relations = await concept.asRemote(tx).getRelations([role]).collect();
              await Promise.all(relations.map(async (relation) => {
                  const rolePlayers = Array.from((await relation.asRemote(tx).getPlayersByRoleType()).entries());
                  await Promise.all(Array.from(rolePlayers, async ([role, setOfThings]) => {
                      // Do not include the current role
                      if (role.getLabel().scopedName() !== this.currentRole) {
                          Array.from(setOfThings.values()).forEach((thing) => {
                              this.relations.get(this.currentRole).otherRolePlayers.push({ role: role.getLabel().name(), player: `${thing.getType().getLabel()}: ${thing.getIID()}` });
                          });
                      }
                  }));
              }));
          } else if (node.typeLabel) {
              const relationType = await tx.concepts().getRelationType(this.relations.get(this.currentRole).relation);
              const roles = await relationType.asRemote(tx).getRelates().collect();
              roles[0].asRemote(tx).getPlayers();
              await Promise.all(roles.map(async role => {
                  if (role.getLabel().scopedName() !== this.currentRole) {
                      const thingTypes = await role.asRemote(tx).getPlayers().collect();
                      thingTypes.forEach(thingType => {
                          this.relations.get(this.currentRole).otherRolePlayers.push({ role: role.getLabel().name(), player: `${thingType.getLabel()}` });
                      });
                  }
              }));
          } else {
              throw "Node must have either an IID or a Label";
          }
        }
      },
    },
  };
</script>

<style scoped>

    .plays-row {
        padding-left: var(--container-padding);
        padding-right: var(--container-padding);
    }

    .column {
        display: flex;
        flex-direction: column;
        align-items: center;
        max-height: 220px;
        padding: var(--container-padding);
        overflow-y: scroll;
        overflow-x: hidden;
    }

    .column::-webkit-scrollbar {
        width: 2px;
    }

    .column::-webkit-scrollbar-thumb {
        background: var(--green-4);
    }

    .row {
        display: flex;
        flex-direction: row;
        align-items: center;
    }

    .roleplayers-list {
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 3px;
    }

    .content {
        padding: var(--container-padding);
        display: flex;
        flex-direction: column;
        overflow: auto;
        justify-content: center;
        border-bottom: var(--container-darkest-border);
    }

    .content-item {
        padding: var(--container-padding);
        display: flex;
        flex-direction: row;
        align-items: center;
    }

    .label {
        margin-right: 20px;
        width: 65px;
    }

    .value {
        width: 110px;
    }

    .role-btn {
        height: 22px;
        min-height: 22px !important;
        cursor: pointer;
        display: flex;
        flex-direction: row;
        width: 100%;
        margin: 0px !important;
        z-index: 2;
    }

    .role-btn-text {
        width: 100%;
        padding-left: 4px;
        display: block;
        white-space: normal !important;
        word-wrap: break-word;
        line-height: 19px;
    }

    .role-btn-caret {
        cursor: pointer;
        align-items: center;
        display: flex;
        min-height: 22px;
        margin: 0px !important;
    }

    .role-list {
        border-left: var(--container-darkest-border);
        border-right: var(--container-darkest-border);
        border-bottom: var(--container-darkest-border);


        background-color: var(--gray-1);
        max-height: 137px;
        overflow: auto;
        position: absolute;
        width: 108px;
        right: 11px;
        margin-top: 0px;
        z-index: 1;
    }

    .role-list-shown {
        border: 1px solid var(--button-hover-border-color) !important;
    }


    .role-list::-webkit-scrollbar {
        width: 2px;
    }

    .role-list::-webkit-scrollbar-thumb {
        background: var(--green-4);
    }

    .role-item {
        align-items: center;
        padding: 2px;
        cursor: pointer;
        white-space: normal;
        word-wrap: break-word;
    }

    .role-item:hover {
        background-color: var(--purple-4);
    }

    /*dynamic*/
    .role-item-selected {
        background-color: var(--purple-3);
    }

</style>
