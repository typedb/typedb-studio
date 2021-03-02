import { GraknOptions } from "../../dependencies_internal";
import options_pb from "grakn-protocol/protobuf/options_pb";
import Options = options_pb.Options;
export declare namespace OptionsProtoBuilder {
    function options(options: GraknOptions): Options;
}
