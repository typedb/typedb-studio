/*
 * Copyright (C) 2021 Vaticle
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

async function conceptToString(concept) {
  let output = '';

  let sup;
  if (concept.isSchemaConcept()) sup = await concept.sup();

  switch (concept) {
    case concept.isAttribute():
      output += `val ${await concept.value()}`;
      break;
    case concept.isSchemaConcept():
      output += `label ${await concept.label()}`;

      if (sup) {
        output += ` sub ${await sup.label()}`;
      }
      break;
    default:
      output += `id ${concept.id}`;
      break;
  }

  if (concept.isRelation()) {
    const rolePlayerList = [];

    const roleplayers = Array.from(((await concept.rolePlayersMap()).entries()));

    // Build array of promises
    const promises = Array.from(roleplayers, async ([role, setOfThings]) => {
      const roleLabel = await role.label();
      await Promise.all(Array.from(setOfThings.values()).map(async (thing) => {
        rolePlayerList.push(`${roleLabel}: id ${thing.id}`);
      }));
    });

    Promise.all(promises).then((() => {
      const relationString = rolePlayerList.join(', ');
      output += ` (${relationString})`;
    }));
  }

  if (concept.isThing()) {
    // const type = await (await concept.type()).label();
    output += ` isa ${await (await concept.type()).label()}`;
  }

  if (concept.isRule()) {
    output += ` when { await ${concept.getWhen()} }`;
    output += ` then { await ${concept.getThen()} }`;
  }
  //
  //   //////////
  //
  //   // Display any requested resources
  //   if (concept.isThing() && attributeTypes.length > 0) {
  //     concept.asThing().attributes(attributeTypes).forEach(resource -> {
  //       String resourceType = colorType(resource.type());
  //       String value = StringUtil.valueToString(resource.value());
  //       output.append(colorKeyword(" has ")).append(resourceType).append(" ").append(value);
  //     });
  //   }
  // ////////

  return output;
}

export default {
  conceptToString,
};
