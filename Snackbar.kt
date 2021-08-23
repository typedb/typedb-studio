import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.visualiser.ui.VisualiserColor

@Composable
fun VaticleSnackbarHost(snackbarHostState : SnackbarHostState){

    val titilliumWeb = FontFamily(
        Font(resource = "fonts/TitilliumWeb/TitilliumWeb-Regular.ttf", weight = FontWeight.W400, style = FontStyle.Normal),
        Font(resource = "fonts/TitilliumWeb/TitilliumWeb-SemiBold.ttf", weight = FontWeight.W700, style = FontStyle.Normal)
    )

    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = {
            // TODO: it's a bit big
            Snackbar (
                backgroundColor = Color(VisualiserColor.RED.argb),
                action = {
                    Text(
                        text = snackbarHostState.currentSnackbarData?.actionLabel?:"",
                        modifier = Modifier
                            .padding(16.dp)
                            .clickable {
                                snackbarHostState.currentSnackbarData?.dismiss()
                            },
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontFamily = titilliumWeb
                        ),
                    )
                }
            ){
                Text(
                    text = snackbarHostState.currentSnackbarData?.message?:"",
                    style = TextStyle(
                        color = Color.Black,
                        fontFamily = titilliumWeb
                    ))
            }
        },
    )
}
