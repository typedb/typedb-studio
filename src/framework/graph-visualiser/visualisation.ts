import { ConstraintVertexAny } from "@typedb/driver-http";
import {
  DataGraph, DataVertex, DataConstraintAny, DataConstraintLinks, DataConstraintHas, DataConstraintIsa,
  DataConstraintOwns, DataConstraintRelates, DataConstraintPlays, DataConstraintSub, DataConstraintFunction,
  DataConstraintExpression, DataConstraintIsaExact, DataConstraintSubExact,
  DataConstraintKind
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
  put_vertex(answer_index: number, vertex: DataVertex, queryVertex: ConstraintVertexAny): void;

  // Edges
  put_isa(answer_index: number, constraint: DataConstraintIsa): void;

  put_isa_exact(answerIndex: number, constraint: DataConstraintIsaExact): void

  put_has(answer_index: number, constraint: DataConstraintHas): void;

  put_links(answer_index: number, constraint: DataConstraintLinks): void;

  put_sub(answer_index: number, constraint: DataConstraintSub): void;

  put_sub_exact(answerIndex: number, constraint: DataConstraintSubExact): void;

  put_owns(answer_index: number, constraint: DataConstraintOwns): void;

  put_relates(answer_index: number, constraint: DataConstraintRelates): void;

  put_plays(answer_index: number, constraint: DataConstraintPlays): void;

  put_expression(answer_index: number, constraint: DataConstraintExpression): void;

  put_function(answer_index: number, constraint: DataConstraintFunction): void;

  put_kind(answer_index: number, constraint: DataConstraintKind): void;
}

export function convertLogicalGraphWith(dataGraph: DataGraph, converter: ILogicalGraphConverter) {
    dataGraph.answers.forEach((edgeList, answerIndex) => {
        edgeList.forEach(edge => {
            putConstraint(converter, answerIndex, edge);
        });
    });
}

function putConstraint(converter: ILogicalGraphConverter, answer_index: number, constraint: DataConstraintAny) {
  switch (constraint.tag) {
    case "isa":{
      converter.put_isa(answer_index, constraint);
      break;
    }
    case "isa!":{
      converter.put_isa_exact(answer_index, constraint);
      break;
    }
    case "has": {
      converter.put_has(answer_index, constraint);
      break;
    }
    case "links": {
      converter.put_links(answer_index, constraint);
      break;
    }
    case "sub": {
      converter.put_sub(answer_index, constraint);
      break;
    }
    case "sub!": {
      converter.put_sub_exact(answer_index, constraint);
      break;
    }
    case "owns": {
      converter.put_owns(answer_index, constraint);
      break;
    }
    case "relates": {
      converter.put_relates(answer_index, constraint);
      break;
    }
    case "plays": {
      converter.put_plays(answer_index, constraint);
      break;
    }
    case "expression" : {
      converter.put_expression(answer_index, constraint);
      break;
    }
    case "function" : {
      converter.put_function(answer_index, constraint);
      break;
    }
    case "kind": {
      converter.put_kind(answer_index, constraint);
      break;
    }
    case "comparison": break;
    case "is": break;
    case "iid": break;
    case "label": break;
    case "value": break;
  }
}
