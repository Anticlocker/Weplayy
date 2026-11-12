package com.sundram.weplay.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sundram.weplay.R
import com.sundram.weplay.data.ProfileStore
import com.sundram.weplay.data.UserProfile
import com.sundram.weplay.data.ageFromDob
import com.sundram.weplay.platform.auth.AuthCredentials
import com.sundram.weplay.platform.auth.LocalAuthRepository
import com.sundram.weplay.platform.auth.ProfileDraft
import com.sundram.weplay.platform.auth.SessionStore
import com.sundram.weplay.ui.components.DobFieldRow
import com.sundram.weplay.ui.components.DobPickerDialog
import com.sundram.weplay.ui.components.FieldLabel
import com.sundram.weplay.ui.components.PlainField
import com.sundram.weplay.ui.components.PrimaryButton
import com.sundram.weplay.ui.components.ProfilePhotoPicker
import com.sundram.weplay.ui.components.RoundIconButton
import com.sundram.weplay.ui.components.UserAvatar
import com.sundram.weplay.ui.theme.AuthSky
import com.sundram.weplay.ui.theme.CardWhite
import com.sundram.weplay.ui.theme.ChipBg
import com.sundram.weplay.ui.theme.CyanBrand
import com.sundram.weplay.ui.theme.CyanDeep
import com.sundram.weplay.ui.theme.CyanInk
import com.sundram.weplay.ui.theme.GoogleBlue
import com.sundram.weplay.ui.theme.OnBrand
import com.sundram.weplay.ui.theme.PageWhite
import com.sundram.weplay.ui.theme.PinkGift
import com.sundram.weplay.ui.theme.Stroke
import com.sundram.weplay.ui.theme.TextBody
import com.sundram.weplay.ui.theme.TextMain
import com.sundram.weplay.ui.theme.TextMuted

private enum class AuthStep { SIGN_IN, PHONE, VERIFY_PHONE, EMAIL, USER_PASS, PROFILE }

@Composable
fun AuthScreen(onDone: (UserProfile) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember {
        LocalAuthRepository(
            store = ProfileStore(context),
            sessions = SessionStore(context),
        )
    }
    var step by rememberSaveable { mutableStateOf(AuthStep.SIGN_IN) }
    var provider by rememberSaveable { mutableStateOf("google") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var dob by rememberSaveable { mutableStateOf(0L) }
    var photoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val pickPhoto = ProfilePhotoPicker { photoPath = it }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AuthSky, PageWhite))
            )
    ) {
        when (step) {
            AuthStep.SIGN_IN -> SignInStep(
                onGoogle = {
                    provider = "google"
                    phone = ""
                    email = ""
                    step = AuthStep.PROFILE
                },
                onPhone = { step = AuthStep.PHONE },
                onEmail = { step = AuthStep.EMAIL },
                onUserPass = { step = AuthStep.USER_PASS },
            )

            AuthStep.USER_PASS -> UserPassStep(
                auth = auth,
                onBack = { step = AuthStep.SIGN_IN },
                onLogin = onDone,
            )

            AuthStep.PHONE -> PhoneStep(
                value = phone,
                onChange = { phone = it },
                onBack = { step = AuthStep.SIGN_IN },
                onContinue = {
                    provider = "phone"
                    email = ""
                    step = AuthStep.VERIFY_PHONE
                },
            )

            AuthStep.VERIFY_PHONE -> PhoneVerifyStep(
                phone = phone,
                onBack = { step = AuthStep.PHONE },
                onVerified = { step = AuthStep.PROFILE },
            )

            AuthStep.EMAIL -> EmailStep(
                value = email,
                onChange = { email = it },
                onBack = { step = AuthStep.SIGN_IN },
                onContinue = {
                    provider = "email"
                    phone = ""
                    step = AuthStep.PROFILE
                },
            )

            AuthStep.PROFILE -> ProfileStep(
                name = name,
                onName = { name = it },
                dob = dob,
                onDob = { dob = it },
                photoPath = photoPath,
                provider = provider,
                onPickPhoto = pickPhoto,
                onBack = { step = AuthStep.SIGN_IN },
                onDone = {
                    onDone(
                        UserProfile(
                            name = name.trim(),
                            dobMillis = dob,
                            id = "WP-" + (100000..999999).random(),
                            photoPath = photoPath,
                            provider = provider,
                            phone = phone.ifBlank { null },
                            email = email.ifBlank { null },
                        )
                    )
                },
            )
        }
    }
}

@Composable
private fun SignInStep(
    onGoogle: () -> Unit,
    onPhone: () -> Unit,
    onEmail: () -> Unit,
    onUserPass: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(38.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(46.dp)
            Spacer(Modifier.width(11.dp))
            Text("WePlay", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Play games.\nMeet people.",
            color = TextMain,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Voice rooms, party games and friends, all in one place.",
            color = TextBody,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Spacer(Modifier.weight(1f))

        GoogleButton(onGoogle)
        Spacer(Modifier.height(12.dp))
        OptionButton("Continue with phone", R.drawable.ic_phone, onPhone)
        Spacer(Modifier.height(12.dp))
        OptionButton("Continue with email", R.drawable.ic_mail, onEmail)
        Spacer(Modifier.height(12.dp))
        OptionButton("Log in with username", R.drawable.ic_users, onUserPass)
        Spacer(Modifier.height(20.dp))
        Text(
            "By continuing you agree to our Terms of Service and Privacy Policy.",
            color = TextMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PhoneStep(
    value: String,
    onChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val valid = value.length == 10
    AuthFormColumn {
        RoundIconButton(
            painter = painterResource(R.drawable.ic_back),
            onClick = onBack,
            contentDescription = "Back",
        )
        Spacer(Modifier.height(26.dp))
        Text("Your phone number", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We will use it to sign you in and keep your account secure.",
            color = TextBody,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(14.dp))
                .border(1.dp, Stroke, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("+91", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .padding(horizontal = 12.dp)
                    .width(1.dp)
                    .height(20.dp)
                    .background(Stroke)
            )
            BasicTextField(
                value = value,
                onValueChange = { input -> onChange(input.filter { it.isDigit() }.take(10)) },
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 16.sp),
                cursorBrush = SolidColor(CyanBrand),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (valid) onContinue() }),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Box {
                        if (value.isEmpty()) {
                            Text("98765 43210", color = TextMuted, fontSize = 16.sp)
                        }
                        field()
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Continue", enabled = valid, onClick = onContinue)
    }
}


@Composable
private fun PhoneVerifyStep(
    phone: String,
    onBack: () -> Unit,
    onVerified: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var sent by rememberSaveable { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val valid = code.length == 6

    AuthFormColumn {
        RoundIconButton(painter = painterResource(R.drawable.ic_back), onClick = onBack, contentDescription = "Back")
        Spacer(Modifier.height(26.dp))
        Text("Verify your number", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enter the 6-digit code sent to +91 $phone. You can continue now; server verification will connect when available.",
            color = TextBody, fontSize = 13.sp, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(14.dp))
                .border(1.dp, Stroke, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = code,
                onValueChange = { input -> error = null; code = input.filter { it.isDigit() }.take(6) },
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(CyanBrand),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid && !checking) onVerified() }),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Box { if (code.isEmpty()) Text("000000", color = TextMuted, fontSize = 20.sp); field() }
                },
            )
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = PinkGift, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (sending) "Sending code…" else if (sent) "Code sent" else "Resend code",
                color = if (sending) TextMuted else CyanInk,
                fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(enabled = !sending) {
                    scope.launch { sending = true; delay(700); sending = false; sent = true }
                }.padding(vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(if (checking) "Checking…" else "Verify and continue", enabled = valid && !checking, onClick = {
            checking = true
            scope.launch { delay(350); checking = false; onVerified() }
        })
    }
}

@Composable
private fun UserPassStep(
    auth: LocalAuthRepository,
    onBack: () -> Unit,
    onLogin: (UserProfile) -> Unit,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val valid = username.isNotBlank() && password.isNotEmpty()

    val submit: () -> Unit = {
        if (!submitting) {
            scope.launch {
                submitting = true
                error = null
                auth.signIn(
                    AuthCredentials(provider = "local", username = username.trim(), password = password),
                ).fold(
                    onSuccess = { _ ->
                        val profile = auth.currentProfile().getOrNull()
                            ?: auth.completeProfile(
                                ProfileDraft(
                                    name = username.trim(),
                                    // Demo default date of birth; the profile screen can update it later.
                                    dobMillis = java.util.Calendar.getInstance()
                                        .apply { add(java.util.Calendar.YEAR, -25) }
                                        .timeInMillis,
                                    provider = "local",
                                ),
                            ).getOrThrow()
                        onLogin(profile)
                    },
                    onFailure = { failure -> error = failure.message ?: "Could not sign in. Try again." },
                )
                submitting = false
            }
        }
    }

    AuthFormColumn {
        RoundIconButton(painter = painterResource(R.drawable.ic_back), onClick = onBack, contentDescription = "Back")
        Spacer(Modifier.height(26.dp))
        Text("Log in to WePlay", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Use any username and password to get in. Accounts are confirmed on the device until the server goes live.",
            color = TextBody, fontSize = 13.sp, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(14.dp))
                .border(1.dp, Stroke, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            BasicTextField(
                value = username,
                onValueChange = { input -> error = null; username = input },
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(CyanBrand),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    Box { if (username.isEmpty()) Text("Username", color = TextMuted, fontSize = 16.sp); field() }
                },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(14.dp))
                .border(1.dp, Stroke, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            BasicTextField(
                value = password,
                onValueChange = { input -> error = null; password = input },
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(CyanBrand),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (valid) submit() }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { field ->
                    Box { if (password.isEmpty()) Text("Password", color = TextMuted, fontSize = 16.sp); field() }
                },
            )
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, color = PinkGift, fontSize = 12.sp)
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton(if (submitting) "Signing in…" else "Log in", enabled = valid && !submitting, onClick = submit)
    }
}

@Composable
private fun EmailStep(
    value: String,
    onChange: (String) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val valid = Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()
    AuthFormColumn {
        RoundIconButton(
            painter = painterResource(R.drawable.ic_back),
            onClick = onBack,
            contentDescription = "Back",
        )
        Spacer(Modifier.height(26.dp))
        Text("Your email", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "We will use it to recover your account and send room invites.",
            color = TextBody,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(26.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(CardWhite, RoundedCornerShape(14.dp))
                .border(1.dp, Stroke, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mail),
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = { onChange(it.take(60)) },
                singleLine = true,
                textStyle = TextStyle(color = TextMain, fontSize = 16.sp),
                cursorBrush = SolidColor(CyanBrand),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (valid) onContinue() }),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    Box {
                        if (value.isEmpty()) {
                            Text("you@example.com", color = TextMuted, fontSize = 16.sp)
                        }
                        field()
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Continue", enabled = valid, onClick = onContinue)
    }
}

@Composable
private fun AuthFormColumn(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ProfileStep(
    name: String,
    onName: (String) -> Unit,
    dob: Long,
    onDob: (Long) -> Unit,
    photoPath: String?,
    provider: String,
    onPickPhoto: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var showDate by remember { mutableStateOf(false) }
    val age = ageFromDob(dob)
    val nameValid = name.trim().length >= 2
    val dobValid = dob > 0L
    val ageValid = age >= 13
    val canContinue = nameValid && dobValid && ageValid

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(
                painter = painterResource(R.drawable.ic_back),
                onClick = onBack,
                contentDescription = "Back",
            )
            Spacer(Modifier.weight(1f))
            Text(
                providerLabel(provider),
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier
                    .background(CardWhite, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Set up your profile", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "This is how you will appear in rooms and games.",
            color = TextBody,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )

        Spacer(Modifier.height(26.dp))
        Box(Modifier.align(Alignment.CenterHorizontally)) {
            UserAvatar(name = name.ifBlank { "?" }, photoPath = photoPath, size = 108.dp)
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(34.dp)
                    .background(CardWhite, CircleShape)
                    .padding(3.dp)
                    .background(CyanBrand, CircleShape)
                    .clickable(onClick = onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "Change photo",
                    tint = OnBrand,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (photoPath == null) "Add a photo" else "Change photo",
            color = CyanInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable(onClick = onPickPhoto)
                .padding(6.dp),
        )

        Spacer(Modifier.height(18.dp))
        FieldLabel("Full name")
        Spacer(Modifier.height(8.dp))
        PlainField(
            value = name,
            onChange = { onName(it.take(24)) },
            placeholder = "Your name",
        )

        Spacer(Modifier.height(18.dp))
        FieldLabel("Date of birth")
        Spacer(Modifier.height(8.dp))
        DobFieldRow(
            dobMillis = dob,
            placeholder = "Select your date of birth",
            onClick = { showDate = true },
        )

        if (dobValid && !ageValid) {
            Spacer(Modifier.height(8.dp))
            Text(
                "You need to be at least 13 years old to join.",
                color = PinkGift,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(26.dp))
        PrimaryButton("Let's go", enabled = canContinue, onClick = onDone)
        Spacer(Modifier.height(24.dp))
    }

    if (showDate) {
        DobPickerDialog(
            initialMillis = dob,
            onConfirm = { onDob(it) },
            onDismiss = { showDate = false },
        )
    }
}

@Composable
private fun GoogleButton(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(CardWhite, RoundedCornerShape(14.dp))
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
            Text("G", color = GoogleBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Continue with Google",
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(22.dp))
    }
}

@Composable
private fun OptionButton(label: String, icon: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .background(CardWhite, RoundedCornerShape(14.dp))
            .border(1.dp, Stroke, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = TextMain,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(20.dp))
    }
}

@Composable
private fun BrandMark(size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .background(
                Brush.linearGradient(listOf(CyanBrand, CyanDeep)),
                RoundedCornerShape(size * 0.28f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "W",
            color = OnBrand,
            fontSize = (size.value * 0.5f).sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

private fun providerLabel(provider: String): String = when (provider) {
    "phone" -> "Signed in with phone"
    "email" -> "Signed in with email"
    else -> "Signed in with Google"
}
