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
  <div class="left-bar-container">
    <div class="minimize-command-bar" @click="toggleCommandsBar">
    </div>
    <div class="command-list">
      <div class="command-item">
      </div>
    </div>
  </div>
</template>

<style scoped>

.left-bar-container {
  background-color: var(--gray-1);
  padding: var(--container-padding);
  border-right: var(--container-darkest-border);
  height: 100%;
  position: relative;
}

.minimize-command-bar {
  background-color: var(--gray-1);
  border-right: var(--container-darkest-border);
  border-top: var(--container-darkest-border);
  border-bottom: var(--container-darkest-border);
  width: 18px;
  height: 22px;
  position: absolute;
  left: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index:1;
}

.command-list {
  display: flex;
  flex-direction: column;
}

.command-item {
  display: flex;
  flex-direction: row;
}

</style>

<script>

export default {
  data() {
    return {
      minimizeCommandsBar: false,
    };
  },
  methods: {
    toggleCommandsBar() {
      this.minimizeCommandsBar = !this.minimizeCommandsBar;
    },
  },
};
</script>
