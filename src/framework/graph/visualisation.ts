import MultiGraph from "graphology";
import Sigma from "sigma";
import {Settings as SigmaSettings} from "sigma/settings";
import {
  Attribute,
  AttributeType,
  ConceptAny,
  EdgeKind,
  Entity,
  EntityType,
  ObjectAny,
  ObjectType,
  Relation,
  RelationType,
  RoleType,
  ThingKind,
  TypeDBValue,
  TypeKind,
} from "./typedb/concept";
import {
  LogicalEdge,
  LogicalGraph,
  LogicalVertex,
  SpecialVertexKind,
  StructureEdgeCoordinates,
  VertexExpression,
  VertexFunction,
  VertexUnavailable
} from "./graph";

export function createSigmaRenderer(containerEl: HTMLElement, sigma_settings: SigmaSettings, graph: MultiGraph) : Sigma {
  // Create the sigma
  return new Sigma(graph, containerEl, sigma_settings);
  // return () => {
  //   renderer.kill();
  // };
}

/////////////////////////////////
// Logical Graph -> Graphology //
/////////////////////////////////

/**
 * You will majorly need:
 *  graph.addNode(id, attributes)
 *  graph.addNode(from, to,  attributes)
 * See: https://www.sigmajs.org/docs/advanced/data/ for attributes
 */
export interface ILogicalGraphConverter {
  // TODO: Functional vertices & edges like expressions, comparisons & function calls

  // Vertices
  put_attribute(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Attribute): void;

  put_entity(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Entity): void;

  put_relation(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Relation): void;

  put_attribute_type(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: AttributeType): void;

  put_entity_type(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: EntityType): void;

  put_relation_type(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: RelationType): void;

  put_role_type_for_type_constraint(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: RoleType): void;

  put_vertex_value(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: TypeDBValue): void;

  put_vertex_expression(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: VertexExpression): void;

  put_vertex_function(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: VertexFunction): void;

  put_vertex_unavailable(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: VertexUnavailable): void;

  // Edges
  put_isa(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void;

  put_has(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, owner: Entity | Relation, attribute: Attribute): void;

  put_links(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, relation: Relation, player: Entity | Relation, role: RoleType | VertexUnavailable): void;

  put_sub(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void;

  put_owns(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, owner: EntityType | RelationType, attribute: AttributeType): void;

  put_relates(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, relation: RelationType, role: RoleType | VertexUnavailable): void;

  put_plays(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, player: EntityType | RelationType, role: RoleType | VertexUnavailable): void;

  put_isa_exact(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void;

  put_sub_exact(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void;

  put_assigned(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, expr_or_func: VertexExpression | VertexFunction, assigned: TypeDBValue, var_name: string): void;

  put_argument(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, argument: TypeDBValue | Attribute, expr_or_func: VertexExpression | VertexFunction, var_name: string): void;
}

export function convertLogicalGraphWith(logicalGraph: LogicalGraph, converter: ILogicalGraphConverter) {
  logicalGraph.answers.forEach((edgeList, answerIndex) => {
    edgeList.forEach(edge => {
      putEdge(converter, answerIndex, edge.structureEdgeCoordinates, edge, logicalGraph);
    });
  });
}

function putVertex(converter: ILogicalGraphConverter, answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: LogicalVertex) {
  switch (vertex.kind) {
    case ThingKind.entity: {
      converter.put_entity(answer_index, structureEdgeCoordinates, vertex as Entity);
      break;
    }
    case ThingKind.attribute : {
      converter.put_attribute(answer_index, structureEdgeCoordinates, vertex as Attribute);
      break;
    }
    case ThingKind.relation : {
      converter.put_relation(answer_index, structureEdgeCoordinates, vertex as Relation);
      break;
    }
    case TypeKind.attributeType : {
      converter.put_attribute_type(answer_index, structureEdgeCoordinates, vertex as AttributeType);
      break;
    }
    case  TypeKind.entityType : {
      converter.put_entity_type(answer_index, structureEdgeCoordinates, vertex as EntityType);
      break;
    }
    case TypeKind.relationType : {
      converter.put_relation_type(answer_index, structureEdgeCoordinates, vertex as RelationType);
      break;
    }
    case TypeKind.roleType : {
      converter.put_role_type_for_type_constraint(answer_index, structureEdgeCoordinates, vertex as RoleType);
      break;
    }
    case "value": {
      converter.put_vertex_value(answer_index, structureEdgeCoordinates, vertex as TypeDBValue);
      break;
    }
    case SpecialVertexKind.unavailable : {
      converter.put_vertex_unavailable(answer_index, structureEdgeCoordinates, vertex as VertexUnavailable);
      break;
    }
    case SpecialVertexKind.func: {
      converter.put_vertex_function(answer_index, structureEdgeCoordinates, vertex as VertexFunction);
      break;
    }
    case SpecialVertexKind.expr: {
      converter.put_vertex_expression(answer_index, structureEdgeCoordinates, vertex as VertexExpression);
      break;
    }

    default : {
      console.log("VertexKind not yet supported: " + vertex);
    }
  }
}

function putEdge(converter: ILogicalGraphConverter, answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, edge: LogicalEdge, logicalGraph: LogicalGraph) {
  let from = logicalGraph.vertices.get(edge.from);
  let to = logicalGraph.vertices.get(edge.to);
  let edgeParam = edge.type.param;
  // First put vertices, then the edge
  putVertex(converter, answer_index, structureEdgeCoordinates, from as ConceptAny);
  putVertex(converter, answer_index, structureEdgeCoordinates, to as ConceptAny);

  switch (edge.type.kind) {
    case EdgeKind.isa:{
      converter.put_isa(answer_index, structureEdgeCoordinates, from as ObjectAny | Attribute, to as ObjectType | AttributeType);
      break;
    }
    case EdgeKind.has: {
      converter.put_has(answer_index, structureEdgeCoordinates, from as ObjectAny, to as Attribute);
      break;
    }
    case EdgeKind.links : {
      converter.put_links(answer_index, structureEdgeCoordinates, from as Relation, to as ObjectAny, edgeParam as RoleType | VertexUnavailable);
      break;
    }

    case EdgeKind.sub: {
      converter.put_sub(answer_index, structureEdgeCoordinates, from as ObjectType | AttributeType, to as ObjectType | AttributeType);
      break;
    }
    case EdgeKind.owns: {
      converter.put_owns(answer_index, structureEdgeCoordinates, from as ObjectType, to as AttributeType);
      break;
    }
    case EdgeKind.relates: {
      converter.put_relates(answer_index, structureEdgeCoordinates, from as RelationType, to as RoleType | VertexUnavailable);
      break;
    }
    case EdgeKind.plays: {
      converter.put_plays(answer_index, structureEdgeCoordinates, from as EntityType | RelationType, to as RoleType | VertexUnavailable);
      break;
    }
    case EdgeKind.isaExact: {
      converter.put_isa_exact(answer_index, structureEdgeCoordinates, from as ObjectAny | Attribute, to as ObjectType | AttributeType);
      break;
    }
    case EdgeKind.subExact: {
      converter.put_sub_exact(answer_index, structureEdgeCoordinates, from as ObjectType, to as ObjectType);
      break;
    }
    case EdgeKind.assigned: {
      converter.put_assigned(answer_index, structureEdgeCoordinates, from as VertexExpression|VertexFunction, to as TypeDBValue, edge.type.param as string);
      break;
    }
    case EdgeKind.argument: {
      converter.put_argument(answer_index, structureEdgeCoordinates, from as TypeDBValue | Attribute, to as VertexExpression|VertexFunction, edge.type.param as string);
      break;
    }

    default : {
      throw new Error();
    }
  }
}
