<template>
    <transition name="fade" appear>
        <div>
            <div class="vis-tabs noselect">
              <div v-for="(tabTitle, tab) in tabs" :key="tab">
                <div :class="(tab === currentTab) ? 'tab current-tab' : 'tab'">
                  <div class="tab-content">
                    <template v-if="tabToRename !== tab">
                      <div @click="toggleTab(tab)" @dblclick="renameTab(tab)" class="tab-title">{{ tabTitle || `Tab ${tab}` }}</div>
                      <div v-if="Object.keys(tabs).length > 1" @click="closeTab(tab)" class="close-tab-btn"><vue-icon className="tab-icon" icon="cross" iconSize="13"></vue-icon></div>
                    </template>

                    <template v-else>
                      <input ref="renameTabInput" class="input-small rename-tab-input" v-model="newTabName">
                      <div @click="cancelRename" class="close-tab-btn"><vue-icon className="tab-icon" icon="cross" iconSize="13"></vue-icon></div>
                      <div  @click="saveName(tab)" class="close-tab-btn"><vue-icon className="tab-icon" icon="tick" iconSize="13"></vue-icon></div>
                    </template>
                  </div>
                </div>
              </div>
              <button v-if="Object.keys(tabs).length < 10" @click="newTab" class='btn new-tab-btn'><vue-icon icon="plus" className="vue-icon"></vue-icon></button>
            </div>

              <div v-for="tab in Object.keys(tabs)" :key="tab">
                <keep-alive>
                  <component v-if="currentTab == tab" :is="visTab" :tabId="tab" :key="tab"></component>
                </keep-alive>
              </div>

        </div>
    </transition>
</template>

<style scoped>

  .tab-content {
    display: flex;
    align-items: center;
  }

  .rename-tab-input {
    width: 60px;
  }

  .new-tab-btn {
    margin-left: 0px !important;
  }

  .tab-title {
    width: 75px;
    height: 100%;
    display: flex;
    align-items: center;
  }

  .vis-tabs {
    position: absolute;
    bottom: 22px;
    z-index: 1;
    width: 100%;
    display: flex;
    align-items: center;
    height: 30px;
    background-color: var(--gray-3);
  }

  .tab {
    background-color: var(--gray-1);
    width: 100px;
    height: 30px;
    border: var(--container-darkest-border);
    border-top: none;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    justify-content: space-between;
    padding-left: var(--container-padding);
    padding-right: var(--container-padding);
  }

  .current-tab {
    background-color: var(--canvas-color);
  }

</style>

<script>

import { mapMutations } from 'vuex';
import VisTab from './VisTab.vue';

export default {
  name: 'VisualiserContent',
  components: { VisTab },
  data() {
    return {
      currentTab: 1,
      tabs: { 1: undefined },
      visTab: 'VisTab',
      LETTER_T_KEYCODE: 84,
      tabToRename: undefined,
      newTabName: '',
    };
  },

  beforeCreate() {
    this.$options.methods = {
      ...(this.$options.methods || {}),
      ...mapMutations(['setActiveTab']),
    };
  },

  created() {
    window.addEventListener('keydown', (e) => {
      // pressing CMD + T will create a new tab
      if ((e.keyCode === this.LETTER_T_KEYCODE) && e.metaKey && Object.keys(this.tabs).length < 10) this.newTab();

      if (this.tabToRename && e.keyCode === 13) this.saveName(this.tabToRename); // Pressing enter will save tab name
      if (this.tabToRename && e.keyCode === 27) this.tabToRename = undefined; // Pressing escape will cancel renaming of tab
    });

    this.setActiveTab('tab-1');
  },
  methods: {
    truncate(name) {
      if (name && name.length > 10) return `${name.substring(0, 10)}...`;
      return name;
    },
    toggleTab(tab) {
      this.currentTab = tab;
      this.cancelRename();
      this.setActiveTab(`tab-${tab}`);
    },
    newTab() {
      const newTabId = Math.max(...Object.keys(this.tabs)) + 1; // Get max tab id and increment it for new tab id
      this.tabs[newTabId] = undefined;
      this.currentTab = newTabId;
      this.setActiveTab(`tab-${newTabId}`);
    },
    async closeTab(tab) {
      const visTabToClose = this.$children.find(x => x.tabId && x.tabId === tab);
      visTabToClose.$destroy();

      // vue doesn't detect property deleteion, so we clone, delete and reassign
      const tabsClone = { ...this.tabs };
      delete tabsClone[tab];
      this.tabs = tabsClone;

      if (this.currentTab === tab) this.currentTab = Object.keys(this.tabs)[0];

      this.setActiveTab(`tab-${this.currentTab}`);
    },
    renameTab(tab) {
      this.tabToRename = tab;
      this.newTabName = this.tabs[tab];
      this.$nextTick(() => this.$refs.renameTabInput[0].focus());
    },
    saveName(tab) {
      this.tabs[tab] = this.truncate(this.newTabName);
      this.cancelRename();
    },
    cancelRename() {
      this.tabToRename = undefined;
      this.newTabName = '';
    },
  },
};
</script>
