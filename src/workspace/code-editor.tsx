import React from "react";
import AceEditor from "react-ace";
import { AceTypeQL } from "./ace-typeql";

export interface CodeEditorProps {
    content: string;
    setContent: (value: string) => any;
}

export const CodeEditor: React.FC<CodeEditorProps> = ({content, setContent}) => {
    const aceEditorRef = React.useRef<AceEditor>(null);

    React.useEffect(() => {
        const customMode = new AceTypeQL();
        aceEditorRef.current.editor.getSession().setMode(customMode as any);
    }, []);

    return <AceEditor ref={aceEditorRef} mode="text" theme="studio-dark" fontSize={"1rem"} value={content}
                      onChange={setContent} width="100%" height="100%"/>
}
