import MultiGraph from "graphology";
import Sigma from "sigma";
import {Settings as SigmaSettings} from "sigma/settings";
import { Attribute, AttributeType, Concept, Entity, EntityType, Relation, RelationType, RoleType, Value } from "../typedb-driver/concept";
import {
    LogicalEdge, LogicalGraph, LogicalVertex, SpecialVertexKind, StructureEdgeCoordinates, VertexExpression,
    VertexFunction, VertexUnavailable
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

  put_vertex_value(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, vertex: Value): void;

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

  put_assigned(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, expr_or_func: VertexExpression | VertexFunction, assigned: Value, var_name: string): void;

  put_argument(answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, argument: Value | Attribute, expr_or_func: VertexExpression | VertexFunction, var_name: string): void;
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
    case "entity": {
      converter.put_entity(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "attribute" : {
      converter.put_attribute(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "relation": {
      converter.put_relation(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "attributeType": {
      converter.put_attribute_type(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "entityType": {
      converter.put_entity_type(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "relationType": {
      converter.put_relation_type(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "roleType": {
      converter.put_role_type_for_type_constraint(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "value": {
      converter.put_vertex_value(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "unavailable": {
      converter.put_vertex_unavailable(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "functionCall": {
      converter.put_vertex_function(answer_index, structureEdgeCoordinates, vertex);
      break;
    }
    case "expression": {
      converter.put_vertex_expression(answer_index, structureEdgeCoordinates, vertex);
      break;
    }

    default: {
      console.log("VertexKind not yet supported: " + vertex);
    }
  }
}

function putEdge(converter: ILogicalGraphConverter, answer_index: number, structureEdgeCoordinates: StructureEdgeCoordinates, edge: LogicalEdge, logicalGraph: LogicalGraph) {
  let from = logicalGraph.vertices.get(edge.from);
  let to = logicalGraph.vertices.get(edge.to);
  let edgeParam = edge.type.param;
  // First put vertices, then the edge
  putVertex(converter, answer_index, structureEdgeCoordinates, from as Concept);
  putVertex(converter, answer_index, structureEdgeCoordinates, to as Concept);

  switch (edge.type.kind) {
    case "isa":{
      converter.put_isa(answer_index, structureEdgeCoordinates, from as Entity | Relation | Attribute, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "has": {
      converter.put_has(answer_index, structureEdgeCoordinates, from as Entity | Relation, to as Attribute);
      break;
    }
    case "links": {
      converter.put_links(answer_index, structureEdgeCoordinates, from as Relation, to as Entity | Relation, edgeParam as RoleType | VertexUnavailable);
      break;
    }
    case "sub": {
      converter.put_sub(answer_index, structureEdgeCoordinates, from as EntityType | RelationType | AttributeType, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "owns": {
      converter.put_owns(answer_index, structureEdgeCoordinates, from as EntityType | RelationType, to as AttributeType);
      break;
    }
    case "relates": {
      converter.put_relates(answer_index, structureEdgeCoordinates, from as RelationType, to as RoleType | VertexUnavailable);
      break;
    }
    case "plays": {
      converter.put_plays(answer_index, structureEdgeCoordinates, from as EntityType | RelationType, to as RoleType | VertexUnavailable);
      break;
    }
    case "isaExact": {
      converter.put_isa_exact(answer_index, structureEdgeCoordinates, from as Entity | Relation | Attribute, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "subExact": {
      converter.put_sub_exact(answer_index, structureEdgeCoordinates, from as EntityType | RelationType | AttributeType, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "assigned": {
      converter.put_assigned(answer_index, structureEdgeCoordinates, from as VertexExpression | VertexFunction, to as Value, edge.type.param as string);
      break;
    }
    case "argument": {
      converter.put_argument(answer_index, structureEdgeCoordinates, from as Value | Attribute, to as VertexExpression | VertexFunction, edge.type.param as string);
      break;
    }
    default: {
      throw new Error();
    }
  }
}
