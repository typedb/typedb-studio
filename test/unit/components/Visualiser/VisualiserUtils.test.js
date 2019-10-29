import {
  limitQuery,
  buildExplanationQuery,
  computeAttributes,
  filterMaps,
  validateQuery,
} from '@/components/Visualiser/VisualiserUtils.js';

import {
  mockedEntityType,
  mockedAttributeType,
  mockedEntityInstance,
  getMockedAnswer,
  mockedAttributeInstance,
  mockedRelationInstance,
  getMockedGraknTx,
} from '../../../helpers/mockedConcepts';

Array.prototype.flatMap = function flat(lambda) { return Array.prototype.concat.apply([], this.map(lambda)); };

jest.mock('@/components/Visualiser/RightBar/SettingsTab/DisplaySettings.js', () => ({
  getTypeLabels() { return []; },
}));

jest.mock('@/components/shared/PersistentStorage', () => ({
  get() { return 10; },
}));

describe('limit Query', () => {
  test('add offset and limit to query', () => {
    const query = 'match $x isa person; get;';
    const limited = limitQuery(query);
    expect(limited).toBe('match $x isa person; get; offset 0; limit 10;');
  });

  test('add offset to query already containing limit', () => {
    const query = 'match $x isa person; get; limit 40;';
    const limited = limitQuery(query);
    expect(limited).toBe('match $x isa person; get; offset 0; limit 40;');
  });

  test('add limit to query already containing offset', () => {
    const query = 'match $x isa person; get; offset 20;';
    const limited = limitQuery(query);
    expect(limited).toBe('match $x isa person; get; offset 20; limit 10;');
  });

  test('query already containing offset and limit does not get changed', () => {
    const query = 'match $x isa person; get; offset 0; limit 40;';
    const limited = limitQuery(query);
    expect(limited).toBe(query);
  });

  test('query already containing offset and limit in inverted order does not get changed', () => {
    const query = 'match $x isa person; get; offset 0; limit 40;';
    const limited = limitQuery(query);
    expect(limited).toBe(query);
  });

  test('query containing multi-line queries', () => {
    const query = `
    match $x isa person;
    $r($x, $y); get;`;
    const limited = limitQuery(query);
    expect(limited).toBe(`
    match $x isa person;
    $r($x, $y); get; offset 0; limit 10;`);
  });

  test('query containing multi-line get query', () => {
    const query = `
    match $x isa person;
    get
         $x;`;
    const limited = limitQuery(query);
    expect(limited).toBe(`
    match $x isa person;
    get
         $x; offset 0; limit 10;`);
  });

  test('query without get', () => {
    const query = 'match $x isa person;';
    const limited = limitQuery(query);
    expect(limited).toBe('match $x isa person;');
  });

  test('multiline query with only limit', () => {
    const query = `match $x isa person;
    get; limit 1;`;
    const limited = limitQuery(query);
    expect(limited).toBe(`match $x isa person;
    get; offset 0; limit 1;`);
  });
});

describe('Compute Attributes', () => {
  test('attach attributes to type', async () => {
    const entityType = { ...mockedEntityType };
    const attributeType = { ...mockedAttributeType };

    const answer = getMockedAnswer([attributeType]);
    const graknTx = getMockedGraknTx([answer]);

    entityType.attributes = () => Promise.resolve({ collect: () => Promise.resolve([mockedAttributeType]) });

    const nodes = await computeAttributes([entityType], graknTx);
    expect(nodes[0].attributes).toHaveLength(1);

    const attrType = nodes[0].attributes[0];
    expect(attrType.type).toBe('some-attribute-type');
  });

  test('attach attributes to thing', async () => {
    const entityInstance = { ...mockedEntityInstance };
    const attributeInstance = { ...mockedAttributeInstance };

    const answer = getMockedAnswer([attributeInstance]);
    const graknTx = getMockedGraknTx([answer]);

    entityInstance.attributes = () => Promise.resolve({ collect: () => Promise.resolve([attributeInstance]) });

    const nodes = await computeAttributes([entityInstance], graknTx);
    expect(nodes[0].attributes).toHaveLength(1);

    expect(nodes[0].attributes[0].type).toBe('some-attribute-type');
    expect(nodes[0].attributes[0].value).toBe('some value');
  });
});

describe('Build Explanation Query', () => {
  test('from two entities', async () => {
    const fistEntityInstance = { ...mockedEntityInstance };
    fistEntityInstance.id = 'ent-1';

    const secondEntityInstance = { ...mockedEntityInstance };
    secondEntityInstance.id = 'ent-2';

    const answer = getMockedAnswer([fistEntityInstance, secondEntityInstance], null);
    const explanationQuery = buildExplanationQuery(answer, '{(role-a: $0, role-b: $1) isa some-relation-type;}');

    expect(explanationQuery.query).toBe('match $0 id ent-1; $1 id ent-2; ');
    expect(explanationQuery.attributeQuery).toBe(null);
  });

  test('from entity and attribute', async () => {
    const entityInstance = { ...mockedEntityInstance };
    const attributeInstance = { ...mockedAttributeInstance };

    const answer = getMockedAnswer([entityInstance, attributeInstance], null);
    const explanationQuery = buildExplanationQuery(answer, '{$1 "male"; $0 has some-attribute-type $1; $0 id ent-instance;}');

    expect(explanationQuery.query).toBe('match $0 id ent-instance; ');
    expect(explanationQuery.attributeQuery).toBe('has some-attribute-type $1;');
  });

  test('from entities and relation', async () => {
    const firstEntityInstance = { ...mockedEntityInstance };
    firstEntityInstance.id = 'ent-1';
    const secondEntityInstance = { ...mockedEntityInstance };
    secondEntityInstance.id = 'ent-2';
    const relationInstance = { ...mockedRelationInstance };
    relationInstance.id = 'rel';

    const answer = getMockedAnswer([firstEntityInstance, secondEntityInstance, relationInstance], null);
    const explanationQuery = buildExplanationQuery(answer, '{$0 id ent-1; $1 id ent-2; $2 (role-a: $0, role-b: $1) isa some-relation-type;}');

    expect(explanationQuery.query).toBe('match $0 id ent-1; $1 id ent-2; $2 id rel; ');
    expect(explanationQuery.attributeQuery).toBe(null);
  });

  test('from attribute and relation', async () => {
    const attributeInstance = { ...mockedAttributeInstance };
    const relationInstance = { ...mockedRelationInstance };

    const answer = getMockedAnswer([attributeInstance, relationInstance], null);

    const explanationQuery = buildExplanationQuery(answer, '{$r has some-attribute-type $1544633775910879; $1544633775910879 < 120;}');
    expect(explanationQuery.query).toBe('match $1 id rel-instance; ');
    expect(explanationQuery.attributeQuery).toBe('has some-attribute-type $0;');
  });
});

describe('Filters out Answers that contain inferred concepts in their ConceptMap', () => {
  test('contains implicit type', async () => {
    const nonImplicitInstance = { ...mockedEntityInstance };
    const implicitInstance = { ...mockedRelationInstance };
    implicitInstance.isImplicit = () => Promise.resolve(true);

    const answer = getMockedAnswer([nonImplicitInstance, implicitInstance]);

    const nonImplicitOnlyAnswer = await filterMaps([answer]);
    expect(nonImplicitOnlyAnswer).toHaveLength(1);
  });

  test('does not contains implicit type', async () => {
    const nonImplicitInstance = { ...mockedEntityInstance };
    const answer = getMockedAnswer([nonImplicitInstance]);

    const nonImplicitOnlyAnswer = await filterMaps([answer]);
    expect(nonImplicitOnlyAnswer).toHaveLength(1);
  });
});

describe('Validate Query', () => {
  test('match get', async () => {
    const query = 'match $p isa person; get;';
    expect(() => {
      validateQuery(query);
    }).not.toThrow();
  });
  test('compute path', async () => {
    const query = 'compute path from V229424, to V446496;';
    expect(() => {
      validateQuery(query);
    }).not.toThrow();
  });
  test('insert', async () => {
    const query = 'insert $x isa emotion; $x "like";';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('match insert', async () => {
    const query = 'match $x ias person, has name "John"; $y isa person, has name "Mary"; insert $r (child: $x, parent: $y) isa parentship;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('match delete', async () => {
    const query = 'match $p isa person, has email "raphael.santos@gmail.com"; delete $p;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('count', async () => {
    const query = 'match $sce isa school-course-enrollment, has score $sco; $sco > 7.0; get; count;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('sum', async () => {
    const query = 'match $org isa organisation, has name $orn; $orn "Medicely"; ($org) isa employment, has salary $sal; get $sal; sum $sal;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('max', async () => {
    const query = 'match $sch isa school, has ranking $ran; get $ran; max $ran;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('min', async () => {
    const query = 'match ($per) isa marriage; ($per) isa employment, has salary $sal; get $sal; min $sal;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('mean', async () => {
    const query = 'match $emp isa employment, has salary $sal; get $sal; mean $sal;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('median', async () => {
    const query = 'match ($per) isa school-course-enrollment, has score $sco; get $sco; median $sco;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('group', async () => {
    const query = 'match $per isa person; $scc isa school-course, has title $tit; (student: $per, enrolled-course: $scc) isa school-course-enrollment; get; group $tit;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('group count', async () => {
    const query = 'match $per isa person; $scc isa school-course, has title $tit; (student: $per, enrolled-course: $scc) isa school-course-enrollment; get; group $tit; count;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
  test('compute', async () => {
    const query = 'compute count in person;';
    expect(() => {
      validateQuery(query);
    }).toThrow();
  });
});
