package com.example.gestionnaire_de_depense.ui.auth

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.gestionnaire_de_depense.auth.presentation.AuthUiState
import com.example.gestionnaire_de_depense.auth.presentation.AuthViewModel
import com.example.gestionnaire_de_depense.R
import com.example.gestionnaire_de_depense.ui.theme.AppTheme
import com.example.gestionnaire_de_depense.ui.theme.PrimaryBlue
import com.example.gestionnaire_de_depense.ui.theme.SecondaryGreen
import kotlin.math.max
import kotlin.random.Random

// Version ViewModel-aware qui expose uniquement les callbacks nécessaires au reste de l'app.
@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthenticated: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isAuthenticated) {
        if (state.isAuthenticated) {
            onAuthenticated()
        }
    }

    AuthContent(
        state = state,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
        onSubmit = viewModel::submit,
        onModeToggle = viewModel::toggleMode,
        onErrorDismissed = viewModel::dismissError,
        modifier = modifier
    )
}

// Variante "stateless" utilisée par la ViewModel et les previews Compose.
@Composable
fun AuthContent(
    state: AuthUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onModeToggle: () -> Unit,
    onErrorDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mode = state.mode
    val isLoading = state.isLoading
    val errorText = state.errorMessage
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AuthBackground(modifier = Modifier.fillMaxSize())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(28.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    AuthLogo()

                    AnimatedContent(
                        // Anime le titre selon le mode pour offrir une transition fluide.
                        targetState = mode,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(240)) +
                                slideInVertically(animationSpec = tween(240)) { it / 3 }) togetherWith
                                (fadeOut(animationSpec = tween(240, easing = FastOutLinearInEasing)) +
                                    slideOutVertically(animationSpec = tween(240)) { it / 3 })
                        },
                        label = "auth-mode"
                    ) { currentMode ->
                        val title = if (currentMode == AuthMode.Login) {
                            "Bienvenue 👋"
                        } else {
                            "Créer un compte ✨"
                        }
                        val subtitle = if (currentMode == AuthMode.Login) {
                            "Connectez-vous pour continuer"
                        } else {
                            "Accédez à votre espace en quelques secondes"
                        }
                        SectionTitle(
                            title = title,
                            subtitle = subtitle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    LabeledTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = "Adresse e-mail",
                        placeholder = "vous@exemple.com",
                        leadingIconRes = R.drawable.ic_mail,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        enabled = !isLoading,
                        isLoading = isLoading
                    )

                    LabeledTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = "Mot de passe",
                        placeholder = "••••••••",
                        leadingIconRes = R.drawable.ic_lock,
                        trailingToggle = PasswordToggle(
                            visible = passwordVisible,
                            onToggle = { passwordVisible = !passwordVisible }
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (mode == AuthMode.Login) ImeAction.Done else ImeAction.Next
                        ),
                        keyboardActions = if (mode == AuthMode.Login) {
                            KeyboardActions(onDone = { onSubmit() })
                        } else {
                            KeyboardActions(onNext = { defaultKeyboardAction(ImeAction.Next) })
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        enabled = !isLoading,
                        isLoading = isLoading
                    )

                    AnimatedVisibility(visible = mode == AuthMode.Register) {
                        LabeledTextField(
                            value = state.confirmPassword,
                            onValueChange = onConfirmPasswordChange,
                            label = "Confirmer le mot de passe",
                            placeholder = "••••••••",
                            leadingIconRes = R.drawable.ic_lock,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                            enabled = !isLoading,
                            isLoading = isLoading
                        )
                    }

                    GradientButton(
                        text = if (mode == AuthMode.Login) "Se connecter" else "Créer un compte",
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canSubmit,
                        isLoading = isLoading
                    )

                    TextButton(
                        onClick = {
                            passwordVisible = false
                            onModeToggle()
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = if (mode == AuthMode.Login) "Pas de compte ? Inscrivez-vous" else "Déjà membre ? Connectez-vous",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }

                    OrDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SocialButton(
                            text = "Google",
                            iconRes = R.drawable.ic_google,
                            modifier = Modifier.weight(1f),
                            onClick = { }
                        )
                        SocialButton(
                            text = "Apple",
                            iconRes = R.drawable.ic_apple,
                            modifier = Modifier.weight(1f),
                            onClick = { }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            visible = errorText != null,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
        ) {
            // Snackbar persistant pour les erreurs réseau/validation.
            Snackbar(
                action = {
                    TextButton(onClick = onErrorDismissed) {
                        Text(text = "OK", color = MaterialTheme.colorScheme.inversePrimary)
                    }
                }
            ) {
                Text(text = errorText.orEmpty())
            }
        }
    }
}

@Composable
private fun AuthLogo(modifier: Modifier = Modifier) {
    val rotation = rememberInfiniteRotation(durationMillis = 8000)
    val colorScheme = MaterialTheme.colorScheme
    val gradientBrush = Brush.linearGradient(
        listOf(PrimaryBlue, colorScheme.tertiaryGlass, SecondaryGreen)
    )
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(brush = gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(rotation) {
                drawArc(
                    color = Color.White.copy(alpha = 0.25f),
                    startAngle = 0f,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = "GD",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun SectionTitle(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)
    val surfaceColor = colorScheme.surface.copy(alpha = 0.92f)
    val borderColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val overlayColors = listOf(
        colorScheme.primary.copy(alpha = 0.12f),
        Color.Transparent,
        colorScheme.tertiary.copy(alpha = 0.08f)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(surfaceColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.linearGradient(overlayColors))
        )
        content()
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "cta-scale"
    )

    OutlinedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent
        ),
        border = null,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        listOf(PrimaryBlue, colorScheme.tertiaryGlass)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = Color.White
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(color = Color.White)
                )
            }
        }
    }
}

data class PasswordToggle(
    val visible: Boolean,
    val onToggle: () -> Unit
)

@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIconRes: Int? = null,
    trailingToggle: PasswordToggle? = null,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        animationSpec = tween(durationMillis = 200),
        label = "border-color"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = 200),
        label = "border-width"
    )

    val fieldShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .border(
                    width = borderWidth,
                    color = borderColor,
                    shape = fieldShape
                ),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIconRes?.let { iconRes ->
                {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            trailingIcon = trailingToggle?.let {
                {
                    IconButton(onClick = it.onToggle) {
                        Icon(
                            painter = painterResource(
                                id = if (it.visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                            ),
                            contentDescription = if (it.visible) "Masquer le mot de passe" else "Afficher le mot de passe"
                        )
                    }
                }
            },
            singleLine = true,
            interactionSource = interactionSource,
            shape = fieldShape,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation
        )

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            ShimmerOverlay(shape = fieldShape)
        }
    }
}

@Composable
fun SocialButton(
    text: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun AuthBackground(modifier: Modifier = Modifier) {
    val offsetAnim = rememberInfiniteTransition(label = "gradient")
    val colorScheme = MaterialTheme.colorScheme
    val gradientColors = listOf(
        PrimaryBlue.copy(alpha = 0.85f),
        colorScheme.tertiaryGlass,
        SecondaryGreen.copy(alpha = 0.8f)
    )
    val shift by offsetAnim.animateFloat(
        initialValue = -0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient-shift"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = Offset(
                x = size.width * shift,
                y = size.height * 0.1f
            )
            val end = Offset(
                x = size.width * (1f - shift),
                y = size.height
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    start = start,
                    end = end
                )
            )
        }

        NoiseOverlay(alpha = 0.04f)
    }
}

@Composable
private fun NoiseOverlay(alpha: Float) {
    val noisePoints = remember {
        val random = Random(0x6E6175)
        List(240) {
            Offset(
                random.nextFloat(),
                random.nextFloat()
            ) to (0.5f + random.nextFloat() * 0.5f)
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "Texture bruitée" }
    ) {
        val radius = max(size.width, size.height) * 0.0025f
        noisePoints.forEach { (normalized, scale) ->
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius * scale,
                center = Offset(
                    normalized.x * size.width,
                    normalized.y * size.height
                )
            )
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(modifier = Modifier.weight(1f))
        Text(
            text = "Ou continuer avec",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Divider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ShimmerOverlay(shape: RoundedCornerShape) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "shimmer-progress"
    )
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .drawBehind {
                val xStart = -size.width
                val xEnd = size.width * 2
                val currentX = lerp(xStart, xEnd, progress)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = shimmerColors,
                        start = Offset(currentX - size.width, 0f),
                        end = Offset(currentX, size.height)
                    )
                )
            }
    )
}

@Composable
private fun rememberInfiniteRotation(durationMillis: Int): Float {
    val transition = rememberInfiniteTransition(label = "logo-rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo-rotation-value"
    )
    return rotation
}

private val ColorScheme.tertiaryGlass: Color
    get() = tertiary.copy(alpha = 0.85f)

@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
private fun LoginPreviewLight() {
    AppTheme(darkTheme = false) {
        AuthContent(
            state = AuthUiState(mode = AuthMode.Login),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onModeToggle = {},
            onErrorDismissed = {}
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun LoginPreviewDark() {
    AppTheme(darkTheme = true) {
        AuthContent(
            state = AuthUiState(mode = AuthMode.Login),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onModeToggle = {},
            onErrorDismissed = {}
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
private fun RegisterPreviewLight() {
    AppTheme(darkTheme = false) {
        AuthContent(
            state = AuthUiState(mode = AuthMode.Register),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onModeToggle = {},
            onErrorDismissed = {}
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RegisterPreviewDarkError() {
    AppTheme(darkTheme = true) {
        AuthContent(
            state = AuthUiState(
                mode = AuthMode.Register,
                errorMessage = "Une erreur est survenue. Réessayez."
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onModeToggle = {},
            onErrorDismissed = {}
        )
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
private fun LoadingPreview() {
    AppTheme(darkTheme = false) {
        AuthContent(
            state = AuthUiState(mode = AuthMode.Login, isLoading = true),
            onEmailChange = {},
            onPasswordChange = {},
            onConfirmPasswordChange = {},
            onSubmit = {},
            onModeToggle = {},
            onErrorDismissed = {}
        )
    }
}
