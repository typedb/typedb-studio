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
    <div class="icon icon--plus" :class="{'is-active':isActive}"></div>
</template>
<script>
export default {
  props: ['isActive'],
};
</script>

<style lang="scss" scoped>
$fs-base : 15px; // define value for 1rem
$s-icon  : 2rem; // change this value to control icon size
 
// ========================================
// Layout
// ========================================

html {
  font-size: $fs-base;
}

body {
  background-color: #2980b9;
}

.icon {
  display: inline-block;
  
  & + & {
    margin-left: 2.5rem;
  }
  
  &__group {
    position: absolute;
    top: 50%;
    left: 0;
    width: 100%;
    transform: translateY(-50%);
    font-size: $s-icon;
    text-align: center;
  }
}

// ========================================
// Helper / Convert px to em
// ========================================

@function em($pixels, $context: $fs-base) {
  @return #{$pixels/$context}em;
}

// ========================================
// Icon Component
// ========================================

$c-icon        : #f0f0f0;
$s-icon-bar    : 2px;
$s-icon-radius : 2px;

.icon {
  position: relative;
  cursor: pointer;

  &:before,
  &:after {
    content: '';
  }

  span,
  &:before,
  &:after {
    position: absolute;
    display: block;
    background-color: $c-icon;
    border-radius: em($s-icon-radius);
    transition: all .2s;
  }
  
// ========================================
// Icon Component / Plus
// ========================================
  
  &--plus,
  &--plus-cross {
    width: 1em;
    height: 1em;
    
    &:before {
      top: 50%;
      left: 0;
      transform: translate3D(0, -50%, 0) rotate(0);
      width: 100%;
      height: em($s-icon-bar);
    }

    &:after {
      top: 50%;
      left: 50%;
      transform: translate3D(-50%, -50%, 0) rotate(0);
      width: em($s-icon-bar);
      height: 100%;
    }
  }
  
  &--plus {
    &.is-active {
      &:before {
        transform: translate3D(0, -50%, 0) rotate(180deg);
      }

      &:after {
        transform: translate3D(-50%, -50%, 0) rotate(90deg);
      }
    }
  }
  
  &--plus-cross {
    &:before,
    &:after {
      transform-origin: center;
    }
    
    &.is-active {
      &:before {
        transform: translate3D(0, -50%, 0) rotate(135deg);
      }

      &:after {
        transform: translate3D(-50%, -50%, 0) rotate(135deg);
      }
    }
  }
}
</style>
