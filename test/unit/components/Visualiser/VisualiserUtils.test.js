import { limitQuery, buildExplanationQuery, computeAttributes, filterMaps, getNeighboursData, validateQuery } from '@/components/Visualiser/VisualiserUtils.js';
import MockConcepts from '../../../helpers/MockConcepts';

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
    const nodes = await computeAttributes([MockConcepts.getMockEntityType()]);
    expect(nodes[0].attributes[0].type).toBe('name');
  });
  test('attach attributes to thing', async () => {
    const nodes = await computeAttributes([MockConcepts.getMockEntity1()]);
    expect(nodes[0].attributes[0].type).toBe('name');
    expect(nodes[0].attributes[0].value).toBe('John');
  });
});

describe('Build Explanation Query', () => {
  test('from two entities', async () => {
    const explanationQuery = buildExplanationQuery(MockConcepts.getMockAnswer1(), MockConcepts.getMockQueryPattern1);
    expect(explanationQuery.query).toBe('match $p id 3333; $c1 id 4444; ');
    expect(explanationQuery.attributeQuery).toBe(null);
  });
  test('from entity and attribute', async () => {
    const explanationQuery = buildExplanationQuery(MockConcepts.getMockAnswer2(), MockConcepts.getMockQueryPattern2);
    expect(explanationQuery.query).toBe('match $c id 3333; ');
    expect(explanationQuery.attributeQuery).toBe('has gender $1234;');
  });
  test('from entity and relation', async () => {
    const explanationQuery = buildExplanationQuery(MockConcepts.getMockAnswer3(), MockConcepts.getMockQueryPattern3);
    expect(explanationQuery.query).toBe('match $p id 3333; $c id 4444; $1234 id 6666; ');
    expect(explanationQuery.attributeQuery).toBe(null);
  });
  test('from attribute and relation', async () => {
    const explanationQuery = buildExplanationQuery(MockConcepts.getMockAnswer4(), MockConcepts.getMockQueryPattern4);
    expect(explanationQuery.query).toBe('match $5678 id 6666; ');
    expect(explanationQuery.attributeQuery).toBe('has duration $1234;');
  });
});

describe('Filters out Answers that contain inferred concepts in their ConceptMap', () => {
  test('contains implicit type', async () => {
    const containsImplicit = await filterMaps([MockConcepts.getMockAnswerContainingImplicitType(), MockConcepts.getMockAnswer1()]);
    expect(containsImplicit).toHaveLength(1);
  });
  test('does not contains implicit type', async () => {
    const containsImplicit = await filterMaps([MockConcepts.getMockAnswer1(), MockConcepts.getMockAnswer2()]);
    expect(containsImplicit).toHaveLength(2);
  });
});

describe('Get neighbours data', () => {
  test('type', async () => {
    const mockGraknTx = {
      query: () => Promise.resolve({ collect: () => Promise.resolve([MockConcepts.getMockAnswerContainingEntity()]) }),
    };
    const neighboursData = await getNeighboursData(MockConcepts.getMockEntityType(), mockGraknTx, 1);

    expect(neighboursData.nodes).toHaveLength(1);
    expect(neighboursData.edges).toHaveLength(1);
    expect(neighboursData.nodes[0]).toMatchObject({ baseType: 'ENTITY', id: '3333', offset: 0, graqlVar: 'x' });
    expect(neighboursData.edges[0]).toEqual(
      { id: '3333-0000-isa', from: '3333', to: '0000', label: '', hiddenLabel: 'isa', arrows: { to: { enable: false } }, options: { hideLabel: true, hideArrow: true } },
    );
  });
  test('entity', async () => {
    const mockGraknTx = {
      query: () => Promise.resolve({ collect: () => Promise.resolve([MockConcepts.getMockAnswerContainingRelation()]) }),
    };
    const neighboursData = await getNeighboursData(MockConcepts.getMockEntity1(), mockGraknTx, 1);

    expect(neighboursData.nodes).toHaveLength(2);
    expect(neighboursData.edges).toHaveLength(2);
    expect(neighboursData.nodes[0]).toMatchObject({ baseType: 'RELATION', id: '6666', explanation: {}, offset: 0, graqlVar: 'r' });
    expect(neighboursData.nodes[1]).toMatchObject({ baseType: 'ENTITY', id: '4444', explanation: {}, graqlVar: 'r' });
    expect(neighboursData.edges[0]).toEqual(
      { id: '6666-3333-son', from: '6666', to: '3333', label: '', hiddenLabel: 'son', arrows: { to: { enable: false } }, options: { hideLabel: true, hideArrow: true } },
    );
    expect(neighboursData.edges[1]).toEqual(
      { id: '6666-4444-father', from: '6666', to: '4444', label: '', hiddenLabel: 'father', arrows: { to: { enable: false } }, options: { hideLabel: true, hideArrow: true } },
    );
  });
  test('attribute', async () => {
    const mockGraknTx = {
      query: () => Promise.resolve({ collect: () => Promise.resolve([MockConcepts.getMockAnswerContainingEntity()]) }),
    };
    const neighboursData = await getNeighboursData(MockConcepts.getMockAttribute(), mockGraknTx, 1);

    expect(neighboursData.nodes).toHaveLength(1);
    expect(neighboursData.edges).toHaveLength(1);
    expect(neighboursData.nodes[0]).toMatchObject({ baseType: 'ENTITY', id: '3333', offset: 0, graqlVar: 'x' });
    expect(neighboursData.edges[0]).toEqual(
      { id: '3333-5555-has', from: '3333', to: '5555', label: '', hiddenLabel: 'has', arrows: { to: { enable: false } }, options: { hideLabel: true, hideArrow: true } },
    );
  });
  test('relation', async () => {
    const mockGraknTx = {
      query: () => Promise.resolve({ collect: () => Promise.resolve([MockConcepts.getMockAnswerContainingEntity()]) }),
      getConcept: () => Promise.resolve((MockConcepts.getMockRelation())),
    };
    const neighboursData = await getNeighboursData(MockConcepts.getMockRelation(), mockGraknTx, 1);

    expect(neighboursData.nodes).toHaveLength(1);
    expect(neighboursData.edges).toHaveLength(1);
    expect(neighboursData.nodes[0]).toMatchObject({ baseType: 'ENTITY', id: '3333', offset: 0, graqlVar: 'x' });
    expect(neighboursData.edges[0]).toEqual(
      { id: '6666-3333-son', from: '6666', to: '3333', label: '', hiddenLabel: 'son', arrows: { to: { enable: false } }, options: { hideLabel: true, hideArrow: true } },
    );
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
