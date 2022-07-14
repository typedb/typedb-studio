// We need to access private function Studio.MainWindow, this allows us to.
// Do not use this outside of tests anywhere. It is extremely dangerous to do so.
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.vaticle.typedb.studio.test


import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.vaticle.typedb.client.TypeDB
import com.vaticle.typedb.client.api.TypeDBOptions
import com.vaticle.typedb.client.api.TypeDBSession
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.studio.Studio
import com.vaticle.typedb.studio.state.StudioState
import com.vaticle.typeql.lang.TypeQL
import com.vaticle.typeql.lang.query.TypeQLMatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Some of these tests use delay!
 *
 * The rationale for this is that substituting in stub classes/methods would create a lot of friction from release to
 * release as the tests would require updating to completely reflect all the internal state that changes with each
 * function. As a heavily state-driven application, duplicating all of this functionality and accurately verifying that
 * the duplicate works in exactly it out of scope.
 *
 * The delays are:
 *  - used only when necessary (some data is travelling between the test and TypeDB)
 *  - generous with the amount of time for the required action
 *
 * However, this is a source of non-determinism and a better and easier way may emerge.
 */
class Experiment {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `Simple Assert Exists`() {
        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent()
            }
            awaitIdle()
            composeRule.onNodeWithText("Open Project").assertExists()
        }
    }

    // This test simulates the carrying out of the instructions found at https://docs.vaticle.com/docs/studio/quickstart
    @Test
    fun `Quickstart`() {
        val schemaString = "define\n" +
                "    repo-id sub attribute,\n" +
                "        value long;\n" +
                "    repo-name sub attribute,\n" +
                "        value string;\n" +
                "    repo-description sub attribute,\n" +
                "        value string;\n" +
                "    \n" +
                "    commit-hash sub attribute,\n" +
                "        value string;\n" +
                "    commit-message sub attribute,\n" +
                "        value string;\n" +
                "    commit-date sub attribute,\n" +
                "        value string;\n" +
                "    \n" +
                "    user-name sub attribute,\n" +
                "        value string;\n" +
                "\n" +
                "    file-name sub attribute,\n" +
                "        value string;\n" +
                "\n" +
                "    repo-file sub relation,\n" +
                "        relates file,\n" +
                "        relates repo;\n" +
                "    \n" +
                "    repo-creator sub relation,\n" +
                "        relates repo,\n" +
                "        relates owner;\n" +
                "\n" +
                "    commit-author sub relation,\n" +
                "        relates author,\n" +
                "        relates commit;\n" +
                "\n" +
                "    commit-file sub relation,\n" +
                "        relates file,\n" +
                "        relates commit;\n" +
                "\n" +
                "    commit-repo sub relation,\n" +
                "        relates commit,\n" +
                "        relates repo;\n" +
                "\n" +
                "    file-collaborator sub relation,\n" +
                "        relates file,\n" +
                "        relates collaborator;\n" +
                "\n" +
                "    repo sub entity,\n" +
                "        plays commit-repo:repo,\n" +
                "        plays repo-creator:repo,\n" +
                "        plays repo-file:repo,\n" +
                "        owns repo-id,\n" +
                "        owns repo-name,\n" +
                "        owns repo-description;\n" +
                "    \n" +
                "    commit sub entity,\n" +
                "        plays commit-author:commit,\n" +
                "        plays commit-file:commit,\n" +
                "        plays commit-repo:commit,\n" +
                "        owns commit-hash,\n" +
                "        owns commit-date;\n" +
                "\n" +
                "    user sub entity,\n" +
                "        plays commit-author:author,\n" +
                "        plays repo-creator:owner,\n" +
                "        plays file-collaborator:collaborator,\n" +
                "        owns user-name;\n" +
                "\n" +
                "    file sub entity,\n" +
                "        plays repo-file:file,\n" +
                "        plays commit-file:file,\n" +
                "        plays file-collaborator:file,\n" +
                "        owns file-name;\n" +
                "\n" +
                "    rule file-collaborator-rule:\n" +
                "        when\n" +
                "    \t{\n" +
                "            (file: \$f, commit: \$c) isa commit-file;\n" +
                "            (commit: \$c, author: \$a) isa commit-author;\n" +
                "        }\n" +
                "    \tthen\n" +
                "    \t{\n" +
                "            (file: \$f, collaborator: \$a) isa file-collaborator;\n" +
                "    \t};"

        val dataString = "insert \$user isa user, has user-name \"dmitrii-ubskii\";\n" +
                "insert \$user isa user, has user-name \"lolski\";\n" +
                "insert \$user isa user, has user-name \"vaticle\";\n" +
                "insert \$user isa user, has user-name \"jmsfltchr\";\n" +
                "insert \$user isa user, has user-name \"krishnangovindraj\";\n" +
                "insert \$user isa user, has user-name \"haikalpribadi\";\n" +
                "match \$user isa user, has user-name \"vaticle\"; insert \$repo isa repo, has repo-id 208812506, has repo-name \"typedb-behaviour\", has repo-description \"TypeDB Behaviour Test Specification\"; \$repo-creator(repo: \$repo, owner: \$user) isa repo-creator; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"concept/type/relationtype.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"concept/type/entitytype.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"typeql/language/undefine.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"typeql/language/define.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"dependencies/vaticle/repositories.bzl\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"typeql/language/rule-validation.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"typeql/reasoner/relation-inference.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"typeql/reasoner/schema-queries.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"concept/type/attributetype.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"typeql/reasoner/negation.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$file isa file, has file-name \"typeql/reasoner/variable-roles.feature\"; \$repo-file(repo: \$repo, file: \$file) isa repo-file; \n" +
                "match \$author isa user, has user-name \"krishnangovindraj\"; \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$commit isa commit, has commit-hash \"8c92af7cd6dd6fc84dc7238cd7ddf0748d5531b1\", has commit-date \"Wed Jun 08 17:13:09 BST 2022\"; \$commit-author(commit: \$commit, author: \$author) isa commit-author; \$commit-repo(commit: \$commit, repo: \$repo) isa commit-repo; \n" +
                "match \$file isa file, has file-name \"typeql/reasoner/negation.feature\"; \$commit isa commit, has commit-hash \"8c92af7cd6dd6fc84dc7238cd7ddf0748d5531b1\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$author isa user, has user-name \"lolski\"; \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$commit isa commit, has commit-hash \"e3efb4813cd4baa7b80d976045fd1c81ffdf81ca\", has commit-date \"Fri Jun 03 16:12:45 BST 2022\"; \$commit-author(commit: \$commit, author: \$author) isa commit-author; \$commit-repo(commit: \$commit, repo: \$repo) isa commit-repo; \n" +
                "match \$file isa file, has file-name \"dependencies/vaticle/repositories.bzl\"; \$commit isa commit, has commit-hash \"e3efb4813cd4baa7b80d976045fd1c81ffdf81ca\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$author isa user, has user-name \"jmsfltchr\"; \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$commit isa commit, has commit-hash \"2a712c4470ccaaaa9f8d7aa5f70b114385c0a47a\", has commit-date \"Wed May 25 12:03:18 BST 2022\"; \$commit-author(commit: \$commit, author: \$author) isa commit-author; \$commit-repo(commit: \$commit, repo: \$repo) isa commit-repo; \n" +
                "match \$file isa file, has file-name \"typeql/language/rule-validation.feature\"; \$commit isa commit, has commit-hash \"2a712c4470ccaaaa9f8d7aa5f70b114385c0a47a\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$file isa file, has file-name \"typeql/reasoner/negation.feature\"; \$commit isa commit, has commit-hash \"2a712c4470ccaaaa9f8d7aa5f70b114385c0a47a\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$file isa file, has file-name \"typeql/reasoner/relation-inference.feature\"; \$commit isa commit, has commit-hash \"2a712c4470ccaaaa9f8d7aa5f70b114385c0a47a\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$file isa file, has file-name \"typeql/reasoner/schema-queries.feature\"; \$commit isa commit, has commit-hash \"2a712c4470ccaaaa9f8d7aa5f70b114385c0a47a\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$file isa file, has file-name \"typeql/reasoner/variable-roles.feature\"; \$commit isa commit, has commit-hash \"2a712c4470ccaaaa9f8d7aa5f70b114385c0a47a\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$author isa user, has user-name \"dmitrii-ubskii\"; \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$commit isa commit, has commit-hash \"6e462bcbef73c75405264777069a22bca696a644\", has commit-date \"Tue May 24 13:03:09 BST 2022\"; \$commit-author(commit: \$commit, author: \$author) isa commit-author; \$commit-repo(commit: \$commit, repo: \$repo) isa commit-repo; \n" +
                "match \$file isa file, has file-name \"concept/type/attributetype.feature\"; \$commit isa commit, has commit-hash \"6e462bcbef73c75405264777069a22bca696a644\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$file isa file, has file-name \"concept/type/entitytype.feature\"; \$commit isa commit, has commit-hash \"6e462bcbef73c75405264777069a22bca696a644\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$file isa file, has file-name \"concept/type/relationtype.feature\"; \$commit isa commit, has commit-hash \"6e462bcbef73c75405264777069a22bca696a644\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$author isa user, has user-name \"haikalpribadi\"; \$repo isa repo, has repo-name \"typedb-behaviour\"; insert \$commit isa commit, has commit-hash \"184bc8a64aa69e383bf496c70b11f02201d33616\", has commit-date \"Fri May 13 20:24:46 BST 2022\"; \$commit-author(commit: \$commit, author: \$author) isa commit-author; \$commit-repo(commit: \$commit, repo: \$repo) isa commit-repo; \n" +
                "match \$file isa file, has file-name \"typeql/language/define.feature\"; \$commit isa commit, has commit-hash \"184bc8a64aa69e383bf496c70b11f02201d33616\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;\n" +
                "match \$file isa file, has file-name \"typeql/language/undefine.feature\"; \$commit isa commit, has commit-hash \"184bc8a64aa69e383bf496c70b11f02201d33616\";insert \$commit-file(commit: \$commit, file: \$file) isa commit-file;"

        runComposeRule(composeRule) {
            setContent {
                Studio.MainWindowContent()
            }

            composeRule.waitForIdle()

            // This opens a dialog box (which we can't see through) so we assert that buttons with that text can be
            // clicked.
            composeRule.onAllNodesWithText("Connect to TypeDB").assertAll(hasClickAction())

            StudioState.client.tryConnectToTypeDB("localhost:1729") {}
            // We wait to connect to TypeDB. This can be slow by default on macOS, so we wait a while.
            delay(5_000)
            assertTrue(StudioState.client.isConnected)

            // Same as connecting to typedb, but we can't see dropdowns either.
            composeRule.onAllNodesWithText("Select Database").assertAll(hasClickAction())

            StudioState.client.tryDeleteDatabase("github")
            delay(500)

            StudioState.client.tryCreateDatabase("github") {}
            // We wait to create the github database.
            delay(500)

            StudioState.client.tryOpenSession("github")
            // We wait to open the session.
            delay(500)


            // This doesn't work because runQuery doesn't source transaction/session types from the GUI.
            // We could click the relevant buttons as assertions they exist and are clickable and then do a pure-state
            // query run.

            // Could probably also store the file locally, include it in the test and open the file through the
            // project browser then use the GUI to operate.

            StudioState.project.tryOpenProject(File("./test/data").toPath())
            StudioState.appData.project.path = File("./test/data").toPath()
            composeRule.waitForIdle()
            delay(500)

            // Attempting to click these throws an errors since we use a pointer system that requires existence of a
            // window/awt backed API, but we can't use windows/awt because of limitations in the testing framework.

            // But we can assert that they exist, which is a test unto itself.

            composeRule.onNodeWithText("schema_string.tql").assertExists()
            composeRule.onNodeWithText("data_string.tql").assertExists()

            composeRule.onNodeWithText("schema").performClick()
            composeRule.onNodeWithText("write").performClick()
            composeRule.waitForIdle()

            StudioState.client.session.tryOpen("github", TypeDBSession.Type.SCHEMA)
            delay(500)
            StudioState.client.tryUpdateTransactionType(TypeDBTransaction.Type.WRITE)
            StudioState.client.session.transaction.runQuery(schemaString)
            delay(500)

            // Commit the schema write.
            // Switch these two statements when we can use windows.
//            composeRule.onNodeWithText(CHECK_STRING).performClick()
            StudioState.client.session.transaction.commit()
            delay(500)

//            composeRule.onAllNodesWithText("data").assertAll(hasClickAction())
            composeRule.onNodeWithText("write").performClick()
            composeRule.waitForIdle()

            StudioState.client.session.tryOpen("github", TypeDBSession.Type.DATA)
            delay(500)
            StudioState.client.session.transaction.runQuery(dataString)
            delay(500)
//            composeRule.onNodeWithText(CHECK_STRING).performClick()
            StudioState.client.session.transaction.commit()

            composeRule.onNodeWithText("infer").performClick()
            composeRule.onNodeWithText("read").performClick()

            delay(250)

            TypeDB.coreClient("localhost:1729").use { client ->
                client.session("github", TypeDBSession.Type.DATA, TypeDBOptions.core().infer(true)).use { session ->
                    session.transaction(TypeDBTransaction.Type.READ).use { transaction ->
                        val results = ArrayList<String>()
                        val query = TypeQL.parseQuery<TypeQLMatch>("match \$file isa file, has file-name \"typeql/reasoner/negation.feature\"; \n" +
                                "\$file-collaborator(file: \$file, collaborator: \$c) isa file-collaborator; \n" +
                                "\$c has user-name \$user-name;")
                        transaction.query().match(query).forEach { result ->
                            results.add(
                                result.get("user-name").asAttribute().value.toString()
                            )
                        }
                        assertEquals(results.size, 2)
                        assertTrue(results.contains("jmsfltchr"))
                        assertTrue(results.contains("krishnangovindraj"))
                    }
                }
            }

        }
    }

    companion object {
        val CLOSE_TRANSACTION_STRING = Char(0xf00du).toString()
        val ROLLBACK_STRING = Char(0xf2eau).toString()
        val CHECK_STRING = Char(0xf00cu).toString()

        val PLAY_STRING = Char(0xf04bu).toString()
        val BOLT_STRING = Char(0xf0e7u).toString()
    }
}

fun runComposeRule(compose: ComposeContentTestRule, rule: suspend ComposeContentTestRule.() -> Unit) {
    runBlocking { compose.rule() }
}