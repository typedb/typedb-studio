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
  <transition name="slide-fade" appear>
        <div class="wrapper">
            <app-bar v-if="showAppBar"></app-bar>
            <div class="main-wrapper">
                <div class="content">
                    <keep-alive>
                        <router-view></router-view>
                    </keep-alive>
                </div>
            </div>
        </div>
    </transition>
</template>

<style scoped>
.slide-fade-enter-active {
    transition: all 1s ease;
}
.slide-fade-enter,
.slide-fade-leave-active {
    opacity: 0;
}

.wrapper {
    display: flex;
    flex-direction: column;
    height: 100vh;
    flex: 1;
}
.main-wrapper {
    display: flex;
    flex-direction: row;
    height: 100vh;
}

.content {
    display: flex;
    flex-direction: row;
    flex: 1;
    position: relative;
}

</style>

<script>
import AppBar from './Appbar.vue';

export default {
  name: 'MainTemplate',
  components: { AppBar },
  computed: {
    showAppBar() {
      return (process.platform === 'darwin');
    },
  },
};
</script>
