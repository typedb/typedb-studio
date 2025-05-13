import MultiGraph from "graphology";
import Sigma from "sigma";

import { Attribute, AttributeType, Concept, Entity, EntityType, Relation, RelationType, RoleType, Value } from "../typedb-driver/concept";
import { QueryEdge, QueryVertex } from "../typedb-driver/query-structure";
import {
    DataEdge, DataGraph, DataVertex, SpecialVertexKind, QueryCoordinates, VertexExpression,
    VertexFunction, VertexUnavailable
} from "./graph";

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
  put_vertex(answer_index: number, vertex: DataVertex, queryVertex: QueryVertex): void;

  // Edges
  put_isa(answer_index: number, edge: DataEdge, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void;

  put_has(answer_index: number, edge: DataEdge, owner: Entity | Relation, attribute: Attribute): void;

  put_links(answer_index: number, edge: DataEdge, relation: Relation, player: Entity | Relation, role: RoleType | VertexUnavailable): void;

  put_sub(answer_index: number, edge: DataEdge, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void;

  put_owns(answer_index: number, edge: DataEdge, owner: EntityType | RelationType, attribute: AttributeType): void;

  put_relates(answer_index: number, edge: DataEdge, relation: RelationType, role: RoleType | VertexUnavailable): void;

  put_plays(answer_index: number, edge: DataEdge, player: EntityType | RelationType, role: RoleType | VertexUnavailable): void;

  put_isa_exact(answer_index: number, edge: DataEdge, thing: Entity | Relation | Attribute, type: EntityType | RelationType | AttributeType): void;

  put_sub_exact(answer_index: number, edge: DataEdge, subtype: EntityType | RelationType | AttributeType, supertype: EntityType | RelationType | AttributeType): void;

  put_assigned(answer_index: number, edge: DataEdge, expr_or_func: VertexExpression | VertexFunction, assigned: Value, var_name: string): void;

  put_argument(answer_index: number, edge: DataEdge, argument: Value | Attribute, expr_or_func: VertexExpression | VertexFunction, var_name: string): void;
}

export function convertLogicalGraphWith(dataGraph: DataGraph, converter: ILogicalGraphConverter) {
    dataGraph.answers.forEach((edgeList, answerIndex) => {
        edgeList.forEach(edge => {
            putEdge(converter, answerIndex, edge, dataGraph);
        });
    });
}

function putEdge(converter: ILogicalGraphConverter, answer_index: number, edge: DataEdge, logicalGraph: DataGraph) {
  let from = logicalGraph.vertices.get(edge.from);
  let to = logicalGraph.vertices.get(edge.to);
  let edgeParam = edge.type.param;
  // First put vertices, then the edge
  converter.put_vertex(answer_index, from as Concept, edge.queryEdge.from);
  converter.put_vertex(answer_index, to as Concept, edge.queryEdge.to);

  switch (edge.type.kind) {
    case "isa":{
      converter.put_isa(answer_index, edge, from as Entity | Relation | Attribute, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "has": {
      converter.put_has(answer_index, edge, from as Entity | Relation, to as Attribute);
      break;
    }
    case "links": {
      converter.put_links(answer_index, edge, from as Relation, to as Entity | Relation, edgeParam as RoleType | VertexUnavailable);
      break;
    }
    case "sub": {
      converter.put_sub(answer_index, edge, from as EntityType | RelationType | AttributeType, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "owns": {
      converter.put_owns(answer_index, edge, from as EntityType | RelationType, to as AttributeType);
      break;
    }
    case "relates": {
      converter.put_relates(answer_index, edge, from as RelationType, to as RoleType | VertexUnavailable);
      break;
    }
    case "plays": {
      converter.put_plays(answer_index, edge, from as EntityType | RelationType, to as RoleType | VertexUnavailable);
      break;
    }
    case "isaExact": {
      converter.put_isa_exact(answer_index, edge, from as Entity | Relation | Attribute, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "subExact": {
      converter.put_sub_exact(answer_index, edge, from as EntityType | RelationType | AttributeType, to as EntityType | RelationType | AttributeType);
      break;
    }
    case "assigned": {
      converter.put_assigned(answer_index, edge, from as VertexExpression | VertexFunction, to as Value, edge.type.param as string);
      break;
    }
    case "argument": {
      converter.put_argument(answer_index, edge, from as Value | Attribute, to as VertexExpression | VertexFunction, edge.type.param as string);
      break;
    }
    default: {
      throw new Error();
    }
  }
}
