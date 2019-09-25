export const META_LABELS = new Set(['entity', 'relation', 'attribute', 'role', 'thing']);

export const baseTypes = {
  TYPE: 'META_TYPE',
  ENTITY_TYPE: 'ENTITY_TYPE',
  RELATION_TYPE: 'RELATION_TYPE',
  ATTRIBUTE_TYPE: 'ATTRIBUTE_TYPE',
  RULE: 'RULE',
  ROLE: 'ROLE',
  ENTITY_INSTANCE: 'ENTITY',
  RELATION_INSTANCE: 'RELATION',
  ATTRIBUTE_INSTANCE: 'ATTRIBUTE',
};

export const sameEdgeCriteria = (edgeA, edgeB) => edgeA.from === edgeB.from && edgeA.to === edgeB.to && edgeA.hiddenLabel === edgeB.hiddenLabel && edgeA.label === edgeB.label;
