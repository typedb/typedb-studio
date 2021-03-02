/*
 * Copyright (C) 2021 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

const networkOptions = {
  autoResize: true,
  physics: {
    barnesHut: {
      springLength: 100,
      springConstant: 0.05,
    },
    minVelocity: 1,
  },
  interaction: {
    hover: true,
    multiselect: true,
  },
  layout: {
    improvedLayout: false,
    randomSeed: 10,
  },
};

export default {
  networkOptions,
};
