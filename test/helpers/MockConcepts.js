/* eslint-disable no-use-before-define */
function getMockEntityType() {
  return {
    baseType: 'ENTITY_TYPE',
    id: '0000',
    label: () => Promise.resolve('person'),
    instances: () => Promise.resolve({ next: () => Promise.resolve(getMockEntity1()) }), // eslint-disable-line no-use-before-define
    isImplicit: () => Promise.resolve(false),
    attributes: () => Promise.resolve({ next: () => Promise.resolve(getMockAttributeType()), collect: () => Promise.resolve([getMockAttributeType()]) }), // eslint-disable-line no-use-before-define
    playing: () => Promise.resolve({ next: () => Promise.resolve(getMockRoleType()), collect: () => Promise.resolve([getMockRoleType()]) }), // eslint-disable-line no-use-before-define
    isType: () => true,
    offset: 0,
  };
}

function getMockRoleType() {
  return {
    baseType: 'Role_TYPE',
    id: 'rrrr',
    label: () => Promise.resolve('child'),
    isType: () => true,
  };
}

function getMockAttributeType() {
  return {
    baseType: 'ATTRIBUTE_TYPE',
    id: '1111',
    label: () => Promise.resolve('name'),
    isImplicit: () => Promise.resolve(false),
    isType: () => true,
    dataType: () => Promise.resolve('String'),
  };
}

function getMockRelationType() {
  return {
    baseType: 'RELATION_TYPE',
    id: '2222',
    label: () => Promise.resolve('parentship'),
    isImplicit: () => Promise.resolve(false),
  };
}

function getMockImplicitRelationType() {
  return {
    baseType: 'RELATION_TYPE',
    id: '2222',
    isImplicit: () => Promise.resolve(true),
    isThing: () => false,
  };
}

function getMockEntity1() {
  return {
    baseType: 'ENTITY',
    id: '3333',
    type: () => Promise.resolve(getMockEntityType()),
    relations: () => Promise.resolve({ next: () => Promise.resolve(getMockRelation()) }), // eslint-disable-line no-use-before-define
    attributes: () => Promise.resolve({ next: () => Promise.resolve(getMockAttribute()), collect: () => Promise.resolve([getMockAttribute()]) }), // eslint-disable-line no-use-before-define
    isType: () => false,
    isInferred: () => Promise.resolve(false),
    isAttribute: () => false,
    isEntity: () => true,
    isThing: () => true,
    isRelation: () => false,
    offset: 0,
  };
}

function getMockEntity2() {
  return {
    baseType: 'ENTITY',
    id: '4444',
    type: () => Promise.resolve(getMockEntityType()),
    isAttribute: () => false,
    isEntity: () => true,
    isThing: () => true,
    isInferred: () => Promise.resolve(false),
    isRelation: () => false,
    isType: () => false,
  };
}

function getMockAttribute() {
  return {
    baseType: 'ATTRIBUTE',
    id: '5555',
    type: () => Promise.resolve(getMockAttributeType()),
    value: () => Promise.resolve('John'),
    owners: () => Promise.resolve({ next: () => Promise.resolve(getMockEntity1()), collect: () => Promise.resolve([getMockEntity1()]) }),
    attributes: () => Promise.resolve({ next: () => Promise.resolve(getMockAttribute()), collect: () => Promise.resolve([getMockAttribute()]) }), // eslint-disable-line no-use-before-define
    isType: () => false,
    isInferred: () => Promise.resolve(false),
    isAttribute: () => true,
    isThing: () => true,
    isRelation: () => false,
    offset: 0,
  };
}

function getMockRelation() {
  const mockRole1 = { label: () => Promise.resolve('son') };
  const mockRole2 = { label: () => Promise.resolve('father') };
  const mockRolePlayers = new Map();
  mockRolePlayers.set(mockRole1, new Set([getMockEntity1()]));
  mockRolePlayers.set(mockRole2, new Set([getMockEntity2()]));

  return {
    baseType: 'RELATION',
    id: '6666',
    type: () => Promise.resolve(getMockRelationType()),
    rolePlayersMap: () => Promise.resolve(mockRolePlayers),
    attributes: () => Promise.resolve({ next: () => Promise.resolve(getMockAttribute()), collect: () => Promise.resolve([getMockAttribute()]) }), // eslint-disable-line no-use-before-define
    isType: () => false,
    isInferred: () => Promise.resolve(false),
    isAttribute: () => false,
    isEntity: () => false,
    isThing: () => true,
    isRelation: () => true,
    explanation: { answers: () => [getMockAnswer1(), getMockAnswer2()], queryPattern: () => '{$y id 1234; $r (father: $m, role: $y) isa parentship; $m id 4444;}' },
    offset: 0,
  };
}

function getMockImplicitRelation() {
  return {
    baseType: 'RELATION',
    id: '6666',
    type: () => Promise.resolve(getMockImplicitRelationType()),
    isThing: () => true,
  };
}

function getMockAnswer1() {
  return {
    explanation: {},
    map: () => {
      const map = new Map();
      map.set('p', getMockEntity1());
      map.set('c1', getMockEntity2());
      return map;
    },
  };
}

const getMockQueryPattern1 = '{(child: $c1, parent: $p) isa parentship;}';

function getMockAnswer2() {
  return {
    explanation: () => ({ answers: () => [getMockAnswer1()], queryPattern: () => 'mock pattern' }),
    map: () => {
      const map = new Map();
      map.set('c', getMockEntity1());
      map.set('1234', getMockAttribute());
      return map;
    },
  };
}

const getMockQueryPattern2 = '{$1234 "male"; $c has gender $1234; $c id V4444;}';

function getMockAnswer3() {
  return {
    explanation: () => {},
    map: () => {
      const map = new Map();
      map.set('p', getMockEntity1());
      map.set('c', getMockEntity2());
      map.set('1234', getMockRelation());
      return map;
    },
  };
}

const getMockQueryPattern3 = '{$c id 4444; $p id 3333; $1234 (child: $c, parent: $p) isa parentship;}';


function getMockAnswer4() {
  return {
    explanation: () => {},
    map: () => {
      const map = new Map();
      map.set('1234', getMockAttribute());
      map.set('5678', getMockRelation());
      return map;
    },
  };
}

const getMockQueryPattern4 = '{$r has duration $1544633775910879; $1544633775910879 < 120;}';

function getMockAnswerContainingImplicitType() {
  return {
    explanation: () => {},
    map: () => {
      const map = new Map();
      map.set('p', getMockEntity1());
      map.set('c', getMockEntity2());
      map.set('r', getMockImplicitRelation());
      return map;
    },
  };
}

function getMockAnswerContainingRelation() {
  return {
    explanation: () => ({ answers: () => [getMockAnswer1()], queryPattern: () => 'mock pattern' }),
    map: () => {
      const map = new Map();
      map.set('r', getMockRelation());
      map.set('c', getMockEntity2());
      return map;
    },
  };
}

function getMockAnswerContainingEntity() {
  return {
    explanation: () => {},
    map: () => {
      const map = new Map();
      map.set('x', getMockEntity1());
      return map;
    },
  };
}

export default {
  getMockEntityType,
  getMockAttributeType,
  getMockRelationType,
  getMockEntity1,
  getMockEntity2,
  getMockAttribute,
  getMockRelation,
  getMockImplicitRelation,
  getMockAnswer1,
  getMockQueryPattern1,
  getMockAnswer2,
  getMockQueryPattern2,
  getMockAnswer3,
  getMockQueryPattern3,
  getMockAnswer4,
  getMockQueryPattern4,
  getMockAnswerContainingImplicitType,
  getMockAnswerContainingRelation,
  getMockAnswerContainingEntity,
};

