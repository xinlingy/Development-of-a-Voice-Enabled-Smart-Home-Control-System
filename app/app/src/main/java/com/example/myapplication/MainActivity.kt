@file:Suppress("DEPRECATION")

package com.example.myapplication

import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.speech.RecognizerIntent
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import android.content.ActivityNotFoundException
import android.app.Activity
import androidx.camera.view.PreviewView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import androidx.compose.ui.zIndex

import com.example.myapplication.network.LoginRequest
import com.example.myapplication.network.LoginResponse
import com.example.myapplication.network.RetrofitInstance
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.network.ControlRequest
import com.example.myapplication.network.ControlResponse
import com.example.myapplication.network.ImagePathResponse

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.widget.Button
import java.io.File
import java.util.*
import java.text.SimpleDateFormat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.InputStream
/*-------------------------------------*/
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import GlobalVariable
import androidx.activity.compose.rememberLauncherForActivityResult
import kotlinx.coroutines.*
import java.util.Locale
var light_state_1 by mutableStateOf(GlobalVariable.Get_Light_state_1())
var light_state_2 by mutableStateOf(GlobalVariable.Get_Light_state_2())
var fan_state_1 by mutableStateOf(GlobalVariable.Get_Fan_state_1())
var AirConditioner_state by mutableStateOf(GlobalVariable.Get_AirConditioner_state())

object AppState {
    var loggedIn by mutableStateOf(false)
    var username by mutableStateOf("")
}

class MainActivity : ComponentActivity() {
    private var recognizedText by mutableStateOf("")
    private var a: Int = 1
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Camera", "Camera Permission Granted")
                startCameraX()
            } else {
                Log.d("Camera", "Camera Permission Denied")
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("Microphone", "Permission Granted")
        } else {
            Log.d("Microphone", "Permission Denied")
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var speechRecognizerLauncher: ActivityResultLauncher<Intent>
    private lateinit var viewFinder: PreviewView
    private lateinit var captureButton: Button
    private lateinit var imageCapture: ImageCapture
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)
        enableEdgeToEdge()

        // Request audio permission at the start
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        // 初始化 speechRecognizerLauncher
        speechRecognizerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val resultText = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (resultText != null && resultText.isNotEmpty()) {
                    recognizedText = resultText[0]
                }
            }
        }


        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFFF6F6F6),
                    bottomBar = {
                        if (AppState.loggedIn) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .background(Color(0xFFF6F6F6))
                                    .clickable{}
                            ) {
                                MicrophoneButton(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(y = (-26).dp) // 向上移動 16dp，可根據需求調整
                                        .padding(16.dp)
                                        .zIndex(-1f),
                                 /*   recognizedText = recognizedText,
                                    onRecognizedText = { /* Handle recognized text */ }*/
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    var selectedControl by remember { mutableStateOf<ControlType?>(null) }

                    if (AppState.loggedIn) {
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .background(Color(0xFFF6F6F6))
                                .fillMaxSize()
                        ) {
                            UserSection(userName = AppState.username, onSettingsClick = {
                                // Navigate to settings screen
                            }, onLogoutClick = {
                                AppState.loggedIn = false
                                AppState.username = ""
                            })

                            // Conditionally show control panel or specific controls
                            when (selectedControl) {
                                null -> ControlPanel(onControlSelected = { controlType -> selectedControl = controlType })
                                ControlType.Fan -> FanControl(onBack = { selectedControl = null }, recognizedText = recognizedText)
                                ControlType.Light -> LightControl(onBack = { selectedControl = null })
                                ControlType.AirConditioner -> AirConditionerControl(onBack = { selectedControl = null })
                                else -> {
                                    Text("Unknown Control Type")
                                }
                            }
                        }
                    } else {
                        LoginScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLogin = { user, password ->
                                handleLogin(user, password) { success ->
                                    if (success) {
                                        AppState.username = user
                                        AppState.loggedIn = true
                                    } else {
                                        Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onGuestLogin = {
                                AppState.username = "Guest"
                                AppState.loggedIn = true
                            },
                            onFaceLogin = {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                startCameraX()
                            }
                        )
                    }
                }
            }
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // 记得调用 super
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                permissionToRecordAccepted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun handleLogin(username: String, password: String, onResult: (Boolean) -> Unit) {
        val loginRequest = LoginRequest(username, password)
        val call = RetrofitInstance.api.login(loginRequest)
        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    val loginResponse = response.body()
                    onResult(loginResponse?.success == true)
                } else {
                    onResult(false)
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginError", "Error: ${t.message}")
                onResult(false)
            }
        })
    }


    private fun startCameraX() {
        setContentView(R.layout.activity_camera)

        viewFinder = findViewById(R.id.viewFinder)
        captureButton = findViewById(R.id.captureButton)

        setupCamera()

        captureButton.setOnClickListener {
            takePhoto()
        }
        val backToLoginButton :ImageButton = findViewById(R.id.backToLoginButton)
        backToLoginButton.setOnClickListener {
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            val frontCameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val fallbackCameraSelector = CameraSelector.Builder().build()

            val cameraSelector = try {
                if (cameraProvider.hasCamera(frontCameraSelector)) {
                    frontCameraSelector
                } else {
                    fallbackCameraSelector
                }
            } catch (e: Exception) {
                Log.e("CameraX", "Error selecting camera", e)
                Toast.makeText(this, "No available cameras", Toast.LENGTH_SHORT).show()
                return@addListener
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraX", "Photo saved at: $savedUri")

                    uploadPhoto(savedUri) { success, username ->
                        if (success) {
                            AppState.loggedIn = true
                            AppState.username = username
                            val intent = Intent(this@MainActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                            Toast.makeText(this@MainActivity, "Login Success", Toast.LENGTH_SHORT).show()
                        } else {
                            // 登录失败，提示错误
                            Toast.makeText(this@MainActivity, "Login Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }


    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    private fun uriToByteArray(uri: Uri, context: Context): ByteArray? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return inputStream?.use { it.readBytes() }
    }
    private fun uploadPhoto(photoUri: Uri, onResult: (Boolean, String) -> Unit) {
        val fileData = uriToByteArray(photoUri, this)

        if (fileData != null) {
            val requestBody = RequestBody.create("application/octet-stream".toMediaTypeOrNull(), fileData)
            val multipartBody = MultipartBody.Part.createFormData("image", "captured_image.jpg", requestBody)


            val call = RetrofitInstance.api.uploadImagePath(multipartBody)

            call.enqueue(object : Callback<ImagePathResponse> {
                override fun onResponse(call: Call<ImagePathResponse>, response: Response<ImagePathResponse>) {
                    if (response.isSuccessful) {
                        val imagePathResponse = response.body()
                        if (imagePathResponse != null && imagePathResponse.success) {
                            onResult(true, imagePathResponse.username)
                        } else {
                            onResult(false, "")
                        }
                    } else {
                        onResult(false, "")
                    }
                }

                override fun onFailure(call: Call<ImagePathResponse>, t: Throwable) {
                    Log.e("Upload", "Network error: ${t.message}")
                    onResult(false, "")
                }
            })
        } else {
            Log.e("Upload", "Failed to convert URI to byte array.")
            onResult(false, "")
        }
    }


    fun startSpeechRecognition() {
        val sttIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.start_to_speak))
        }

        try {
            speechRecognizerLauncher.launch(sttIntent)
        } catch (e: ActivityNotFoundException) {
            Log.d("TAG", "onClick: ${e.localizedMessage}")
            Toast.makeText(this, "Your device does not support STT.", Toast.LENGTH_LONG).show()
        }
    }
}


@Composable
fun MicrophoneButton(modifier: Modifier = Modifier) {
    var isMicrophoneActive by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // 初始化 ActivityResultLauncher
    val speechInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val resultArray = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = resultArray?.get(0)
            // 使用 TTS 或其他方式處理結果
            if (spokenText != null) {
                Log.d("SpeechRecognition", "Recognized text: $spokenText")
                Toast.makeText(context, "您說的是: $spokenText", Toast.LENGTH_SHORT).show()
                // 回應用戶的輸入
                respondToInput(spokenText, context)
                isMicrophoneActive = false
            }
        } else {
            isMicrophoneActive = false // 如果沒有成功識別，則設置為 false
        }
    }
    // 局部函式
    fun handleMicrophoneClick() {
        isMicrophoneActive = true // 啟動語音輸入
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("zh", "TW")) // 繁體中文
        }
        try {
            speechInputLauncher.launch(intent) // 使用 ActivityResultLauncher
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "語音輸入不受支持", Toast.LENGTH_SHORT).show()
        }
    }
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (isMicrophoneActive) Color.Green else Color.Gray)
            .clickable { handleMicrophoneClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_microphone),
            contentDescription = "Microphone",
            modifier = Modifier.size(30.dp)
        )
    }
}


// 語音回應用戶的輸入採用TTS架構
// 語言問題透過調整手機的語言版本即可解決
fun respondToInput(userInput: String, context: Context) {
    var response = ""
    var controlStatus = false
    // 定義正則表達式來提取時間 (例: "開啟電扇 30 秒")
    val timerRegex_fan = Regex("扇計時(\\d+)秒")
    val timerRegex_airconditioner = Regex("冷氣計時(\\d+)秒")
    when {
        userInput.contains("扇計時") && timerRegex_fan.containsMatchIn(userInput) -> {
            val match = timerRegex_fan.find(userInput)
            val durationInSeconds = match?.groups?.get(1)?.value?.toIntOrNull()

            if (match!=null && durationInSeconds != null) {
                controlStatus = true // 設置狀態為開
                controlFan(controlStatus, 1) { success ->
                    if (success) {
                        response = "好的，我已經開啟了電扇，會在 $durationInSeconds 秒後自動關閉"
                        fan_state_1 = true
                        Log.d("ControlFan", "電扇已開啟")

                        // 使用協程處理延遲關閉
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(durationInSeconds * 1000L) // 延遲指定秒數（毫秒）
                            controlFan(false, 1) { turnOffSuccess ->
                                if (turnOffSuccess) {
                                    fan_state_1 = false
                                    Log.d("ControlFan", "電扇已自動關閉")
                                    (context.applicationContext as Initializer).speak("時間到，我已經關閉了電扇")
                                } else {
                                    Log.d("ControlFan", "自動關閉電扇失敗")
                                }
                            }
                        }
                    } else {
                        response = "電扇無法開啟，請確認網路連線或設備狀況"
                        Log.d("ControlFan", "開啟電扇失敗")
                    }
                    (context.applicationContext as Initializer).speak(response)
                }
            } else {
                response = "抱歉，我無法理解開啟電扇的時間，請重新說明"
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("冷氣計時") && timerRegex_airconditioner.containsMatchIn(userInput) -> {
            val match = timerRegex_airconditioner.find(userInput)
            val durationInSeconds = match?.groups?.get(1)?.value?.toIntOrNull()

            if (match!=null && durationInSeconds != null) {
                controlStatus = true // 設置狀態為開
                controlAirConditioner(controlStatus, 1) { success ->
                    if (success) {
                        response = "好的，我已經開啟了冷氣，會在 $durationInSeconds 秒後自動關閉"
                        AirConditioner_state = true
                        Log.d("ControlFan", "冷氣已開啟")

                        // 使用協程處理延遲關閉
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(durationInSeconds * 1000L) // 延遲指定秒數（毫秒）
                            controlAirConditioner(false, 1) { turnOffSuccess ->
                                if (turnOffSuccess) {
                                    AirConditioner_state = false
                                    Log.d("ControlFan", "冷氣已自動關閉")
                                    (context.applicationContext as Initializer).speak("時間到，我已經關閉了冷氣")
                                } else {
                                    Log.d("ControlFan", "自動關閉冷氣失敗")
                                }
                            }
                        }
                    } else {
                        response = "冷氣無法開啟，請確認網路連線或設備狀況"
                        Log.d("ControlFan", "開啟冷氣失敗")
                    }
                    (context.applicationContext as Initializer).speak(response)
                }
            } else {
                response = "抱歉，我無法理解開啟冷氣的時間，請重新說明"
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("開") && userInput.contains("客廳") && userInput.contains("燈")-> {
            controlStatus = true // 設置狀態為開
            controlLight(controlStatus,1) { success ->
                if (success) {
                    response  = "好的，我已經打開了客廳的電燈"
                    light_state_1 = true
                    Log.d("ControlLight", "客廳電燈已開啟")
                } else {
                    response  = "電燈無法開啟，請確認網路連線或設備狀況"
                    Log.d("ControlLight", "開啟客廳電燈失敗")
                }
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("關") && userInput.contains("客廳") && userInput.contains("燈")-> {
            response  = "好的，我已經關閉了客廳的電燈"
            controlStatus = false // 設置狀態為開
            controlLight(controlStatus,1) { success ->
                if (success) {
                    response  = "好的，我已經關閉了客廳的電燈"
                    light_state_1 = false
                    Log.d("ControlLight", "客廳電燈已關閉")
                } else {
                    response  = "電燈無法關閉，請確認網路連線或設備狀況"
                    Log.d("ControlLight", "關閉客廳電燈失敗")
                }
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("開燈") -> {
            response  = "請問您想打開哪裡的電燈 ? "
        }
        userInput.contains("開") && userInput.contains("臥室") && userInput.contains("燈")-> {
        controlStatus = true // 設置狀態為開
        controlLight(controlStatus,2) { success ->
            if (success) {
                response  = "好的，我已經打開了臥室的電燈"
                light_state_2 = true
                Log.d("ControlLight", "臥室電燈已打開")
            } else {
                response  = "電燈無法開啟，請確認網路連線或設備狀況"
                Log.d("ControlLight", "打開臥室電燈失敗")
            }
            (context.applicationContext as Initializer).speak(response)
        }
    }
        userInput.contains("關") && userInput.contains("臥室") && userInput.contains("燈")-> {
            controlStatus = false // 設置狀態為開
            controlLight(controlStatus,2) { success ->
                if (success) {
                    response  = "好的，我已經關閉了臥室的電燈"
                    light_state_2 = false
                    Log.d("ControlLight", "臥室電燈已關閉")
                } else {
                    response  = "電燈無法關閉，請確認網路連線或設備狀況"
                    Log.d("ControlLight", "關閉臥室電燈失敗")
                }
                (context.applicationContext as Initializer).speak(response)
            }
        }

        userInput.contains("關燈") -> {
            response  = "請問您想關閉哪裡的電燈 ? "
        }
        userInput.contains("開")&& userInput.contains("扇") -> {
            controlStatus = true // 設置狀態為開
            controlFan(controlStatus,1) { success ->
                if (success) {
                    response  = "好的，我已經開啟了電扇 "
                    fan_state_1 = true
                    Log.d("ControlLight", "電扇已開啟")
                } else {
                    fan_state_1 = false
                    response  = "電扇無法開啟，請確認網路連線或設備狀況"
                    Log.d("ControlLight", "開啟電扇失敗")
                }
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("關")&& userInput.contains("扇") -> {
            controlStatus = false // 設置狀態為關
            controlFan(controlStatus,1) { success ->
                if (success) {
                    response  = "好的，我已經關閉了電扇 "
                    fan_state_1 = false
                    Log.d("ControlLight", "電扇已關閉")
                } else {
                    response  = "電扇無法關閉，請確認網路連線或設備狀況"
                    Log.d("ControlLight", "關閉電扇失敗")
                }
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("開")&& userInput.contains("冷氣") -> {
            controlStatus = true // 設置狀態為關
            controlAirConditioner(controlStatus,1) { success ->
                if (success) {
                    response  = "好的，我已經開啟了冷氣 "
                    AirConditioner_state = true
                    Log.d("ControlAirConditioner", "冷氣已開啟")
                } else {
                    response  = "冷氣無法開啟，請確認網路連線或設備狀況"
                    Log.d("ControlAirConditioner", "關閉冷氣失敗")
                }
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("關")&& userInput.contains("冷氣") -> {
            controlStatus = false // 設置狀態為關
            controlAirConditioner(controlStatus,1) { success ->
                if (success) {
                    response  = "好的，我已經關閉了冷氣 "
                    AirConditioner_state = false
                    Log.d("ControlAirConditioner", "冷氣已關閉")
                } else {
                    response  = "冷氣無法關閉，請確認網路連線或設備狀況"
                    Log.d("ControlAirConditioner", "關閉冷氣失敗")
                }
                (context.applicationContext as Initializer).speak(response)
            }
        }
        userInput.contains("謝謝") -> {
            response  = "不客氣！"
        }
        else ->{
            response = "抱歉，我不太明白。"
        }
    }
    // 使用 TTS 說出回應
    (context.applicationContext as Initializer).speak(response) // 使用 Initializer 類別的 speak 方法
}



@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLogin: (String, String) -> Unit, onFaceLogin: () -> Unit, onGuestLogin: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onLogin(username, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onFaceLogin() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login with Face")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onGuestLogin() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login as Guest")
        }
    }
}

@Composable
fun UserSection(userName: String, onSettingsClick: () -> Unit, onLogoutClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color(0xFF6200EE), shape = MaterialTheme.shapes.medium)
            .padding(16.dp)
            .clickable { onSettingsClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_user),
                    contentDescription = null
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Welcome,", color = Color.White, fontSize = 20.sp)
                Text(text = userName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = onLogoutClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text(text = "Logout", color = Color(0xFF6200EE))
        }
    }
}
@Composable
fun ControlPanel(onControlSelected: (ControlType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ControlButton(
            label = "Fan",
            iconRes = R.drawable.ic_fan,
            containerColor = Color(0xFF03DAC5)
        ) {
            onControlSelected(ControlType.Fan)
        }
        ControlButton(
            label = "Light",
            iconRes = R.drawable.ic_lightbulb,
            containerColor = Color(0xFFFFC107)
        ) {
            onControlSelected(ControlType.Light)
        }
        ControlButton(
            label = "Air Conditioner", // Add a label for the air conditioner button
            iconRes = R.drawable.ic_air_conditioner, // Add your air conditioner icon resource
            containerColor = Color(0xFF1E88E5) // Choose a color that fits your design
        ) {
            onControlSelected(ControlType.AirConditioner) // Handle the air conditioner control
        }
    }
}

@Composable
fun ControlButton(label: String, iconRes: Int, containerColor: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(containerColor, shape = MaterialTheme.shapes.medium),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FanControl(onBack: () -> Unit, recognizedText: String) {
    var fanSpeed by remember { mutableIntStateOf(0) } // 0: Off, 1: Low, 2: Medium, 3: High
    var hoursInput by remember { mutableStateOf("0") }
    var minutesInput by remember { mutableStateOf("0") }
    var secondsInput by remember { mutableStateOf("0") }
    var isFanOn by remember { mutableStateOf(false) } // Fan on/off state
    var isTimerRunning by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableIntStateOf(0) } // Time left in seconds
    val context = LocalContext.current

    LaunchedEffect(recognizedText) {
        // Initialize variables for control flags
        var tempFanOn = isFanOn
        var tempFanSpeed = fanSpeed
        var tempTimeLeft = timeLeft
        var tempIsTimerRunning = isTimerRunning

        // Check if the recognized text contains commands
        if (recognizedText.contains("開電風扇", ignoreCase = true)) {
            tempFanOn = true
            Toast.makeText(context, "Fan turned on via voice command", Toast.LENGTH_SHORT).show()
        }
        if (recognizedText.contains("關電風扇", ignoreCase = true)) {
            tempFanOn = false
            Toast.makeText(context, "Fan turned off via voice command", Toast.LENGTH_SHORT).show()
        }
        if (recognizedText.contains("弱風", ignoreCase = true)) {
            tempFanSpeed = 1
            Toast.makeText(context, "Fan set to low speed via voice command", Toast.LENGTH_SHORT).show()
        }
        if (recognizedText.contains("中風", ignoreCase = true)) {
            tempFanSpeed = 2
            Toast.makeText(context, "Fan set to medium speed via voice command", Toast.LENGTH_SHORT).show()
        }
        if (recognizedText.contains("強風", ignoreCase = true)) {
            tempFanSpeed = 3
            Toast.makeText(context, "Fan set to high speed via voice command", Toast.LENGTH_SHORT).show()
        }
        if (recognizedText.contains("定時", ignoreCase = true)) {
            // Use regex to extract time: hours, minutes, and seconds
            val timePattern = Regex("(\\d+)小時|(\\d+)分鐘|(\\d+)秒")
            val matchResults = timePattern.findAll(recognizedText)

            // Initialize variables for timer
            var hours = 0
            var minutes = 0
            var seconds = 0

            // Iterate through all matches
            for (match in matchResults) {
                match.groups[1]?.let { hours = it.value.toInt() }  // If hours are found
                match.groups[2]?.let { minutes = it.value.toInt() } // If minutes are found
                match.groups[3]?.let { seconds = it.value.toInt() } // If seconds are found
            }

            // Set the timer based on voice command
            val totalSeconds = hours * 3600 + minutes * 60 + seconds
            tempTimeLeft = totalSeconds
            tempIsTimerRunning = totalSeconds > 0
            if (totalSeconds > 0) tempFanOn = true // Automatically turn on the fan when the timer starts
            Toast.makeText(context, "Timer set for $hours hours, $minutes minutes, $seconds seconds", Toast.LENGTH_SHORT).show()
        }

        // Apply changes to the state variables
        isFanOn = tempFanOn
        fanSpeed = tempFanSpeed
        timeLeft = tempTimeLeft
        isTimerRunning = tempIsTimerRunning
    }


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize() // 充滿可用空間
            .verticalScroll(rememberScrollState())
            .padding(16.dp) // 保留適當邊距
    )
    {
        Text("Fan Control", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Toggle fan on/off
        Button(
            onClick = {
                if (timeLeft == 0) {
                    isFanOn = !isFanOn
                }
                GlobalVariable.Change_Fan_state_1()
                fan_state_1 = GlobalVariable.Get_Fan_state_1()
                controlFanWrapper(fan_state_1,1){success ->
                    if (success) {
                        Log.d("ControlFan", "電扇已開啟")
                    } else {
                        GlobalVariable.Change_Fan_state_1()
                        fan_state_1 = GlobalVariable.Get_Fan_state_1()
                        Log.d("ControlFan", "開啟電扇失敗")
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (fan_state_1) Color.Green else Color.Red
            )
        ) {
            Text(text = if (fan_state_1) "Turn Off" else "Turn On")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Fan Speed")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeedButton("Low", 1, fanSpeed, isFanOn) { fanSpeed = it }
//            SpeedButton("Medium", 2, fanSpeed, isFanOn) { fanSpeed = it }
            SpeedButton("High", 3, fanSpeed, isFanOn) { fanSpeed = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input fields for hours, minutes, and seconds with labels
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Timer", fontSize = 16.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Hours")
                    TextField(
                        value = if (isTimerRunning) "%02d".format(timeLeft / 3600) else hoursInput,
                        onValueChange = { hoursInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.width(80.dp),
                        enabled = !isTimerRunning
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Minutes")
                    TextField(
                        value = if (isTimerRunning) "%02d".format((timeLeft / 60) % 60) else minutesInput,
                        onValueChange = { minutesInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.width(80.dp),
                        enabled = !isTimerRunning
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Seconds")
                    TextField(
                        value = if (isTimerRunning) "%02d".format(timeLeft % 60) else secondsInput,
                        onValueChange = { secondsInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.width(80.dp),
                        enabled = !isTimerRunning
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Timer button
        Button(
            onClick = {
                val totalSeconds = (hoursInput.toIntOrNull() ?: 0) * 3600 +
                        (minutesInput.toIntOrNull() ?: 0) * 60 +
                        (secondsInput.toIntOrNull() ?: 0)

                if (totalSeconds > 0) {
                    timeLeft = totalSeconds
                    isTimerRunning = true
                    isFanOn = true // 計時開始時啟動風扇
                    // 控制風扇打開
                    GlobalVariable.Change_Fan_state_1()
                    fan_state_1 = true
                    controlFanWrapper(fan_state_1, 1) { success ->
                        if (success) {
                            Log.d("ControlFan", "電扇已開啟")
                        } else {
                            GlobalVariable.Change_Fan_state_1()
                            fan_state_1 = false
                            Log.d("ControlFan", "開啟電扇失敗")
                        }
                    }
                } else {
                    fan_state_1 = false
                    Toast.makeText(context, "請輸入有效的時間", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Text("Start")
        }


        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally) // 垂直居中對齊
                .imePadding() // 自動適應鍵盤或其他底部元素
        ) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(32.dp))

    }

    // Simulate the timer countdown
    LaunchedEffect(isTimerRunning, timeLeft) {
        if (isTimerRunning && timeLeft > 0) {
            kotlinx.coroutines.delay(1000L) // 延遲 1 秒
            timeLeft-- // 減少剩餘時間
        } else if (timeLeft == 0) {
            isTimerRunning = false
            isFanOn = false // 計時結束時關閉風扇
            // 控制風扇關閉
            GlobalVariable.Change_Fan_state_1()
            fan_state_1 = false
            controlFanWrapper(fan_state_1, 1) { success ->
                if (success) {
                    Log.d("ControlFan", "電扇已關閉")
                } else {
                    GlobalVariable.Change_Fan_state_1()
                    fan_state_1 = true
                    Log.d("ControlFan", "關閉電扇失敗")
                }
            }
        }
    }

}
private fun controlFanWrapper(status: Boolean, FanNumber: Int, onResult: (Boolean) -> Unit) {
    controlFan(status, FanNumber, onResult)
}

private fun controlFan(status: Boolean, FanNumber: Int, onResult: (Boolean) -> Unit) {
    val request = ControlRequest(status)
    if (FanNumber == 1) {
        RetrofitInstance.lightApi.controlFan(request).enqueue(object : Callback<ControlResponse> {
            override fun onResponse(call: Call<ControlResponse>, response: Response<ControlResponse>) {
                Log.d("ControlFan", "Response received for Fan $FanNumber: ${response.body()}")
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.d("ControlFan", "Response failed with status: ${response.code()}")
                    onResult(false)
                }
            }
            override fun onFailure(call: Call<ControlResponse>, t: Throwable) {
                Log.e("ControlFan", "Request failed for Fan $FanNumber: ${t.message}")
                onResult(false)
            }
        })
    }
}

@Composable
fun SpeedButton(text: String, speed: Int, currentSpeed: Int, isFanOn: Boolean, onSpeedSelected: (Int) -> Unit) {
    Button(
        onClick = { if (isFanOn) onSpeedSelected(speed) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (currentSpeed == speed) Color.DarkGray else Color.LightGray
        ),
        enabled = isFanOn // Disable button if fan is off
    ) {
        Text(text = text)
    }
}

@Composable
fun LightControl(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Light Control", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // 控制第一盞燈
        Switch(
            checked = light_state_1,
            onCheckedChange = { isChecked ->
                GlobalVariable.Change_Light_state_1()
                light_state_1 = GlobalVariable.Get_Light_state_1()
                Log.d("UserInfoA", "LED 1 status: ${light_state_1}")
                controlLightWrapper(isChecked, 1) { success ->
                    if (success) {
                        Toast.makeText(context, "Light 1 status updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        GlobalVariable.Change_Light_state_1()
                        light_state_1 = GlobalVariable.Get_Light_state_1()
                        Log.d("UserInfoB", "Light 1 status failed to update")
                        Toast.makeText(context, "Failed to update Light 1 status", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (light_state_1) Color(0xFFFFC107) else Color.Gray,
                uncheckedThumbColor = if (light_state_1) Color(0xFFFFC107) else Color.Gray,
                checkedTrackColor = if (light_state_1) Color(0xFFFFC107).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f),
                uncheckedTrackColor = if (light_state_1) Color(0xFFFFC107).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f)
            )
        )
        Text(if (light_state_1) "Living room is On" else "Living room is Off")

        // 控制第二盞燈
        Switch(
            checked = light_state_2,
            onCheckedChange = { isChecked ->
                GlobalVariable.Change_Light_state_2()
                light_state_2 = GlobalVariable.Get_Light_state_2()
                Log.d("UserInfoA", "LED 2 status: ${light_state_2}")
                controlLightWrapper(isChecked, 2) { success ->
                    if (success) {
                        Toast.makeText(context, "Light 2 status updated successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        GlobalVariable.Change_Light_state_2()
                        light_state_2 = GlobalVariable.Get_Light_state_2()
                        Log.d("UserInfoB", "Light 2 status failed to update")
                        Toast.makeText(context, "Failed to update Light 2 status", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = if (light_state_2) Color(0xFFFFC107) else Color.Gray,
                uncheckedThumbColor = if (light_state_2) Color(0xFFFFC107) else Color.Gray,
                checkedTrackColor = if (light_state_2) Color(0xFFFFC107).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f),
                uncheckedTrackColor = if (light_state_2) Color(0xFFFFC107).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.5f)
            )
        )
        Text(if (light_state_2) "Bath room is On" else "Bath room is Off")

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
private fun controlLightWrapper(status: Boolean, lightNumber: Int, onResult: (Boolean) -> Unit) {
    controlLight(status, lightNumber, onResult)
}

private fun controlLight(status: Boolean, lightNumber: Int, onResult: (Boolean) -> Unit) {
    val request = ControlRequest(status)
    if (lightNumber == 1) {
        RetrofitInstance.lightApi.controlLight_1(request).enqueue(object : Callback<ControlResponse> {
            override fun onResponse(call: Call<ControlResponse>, response: Response<ControlResponse>) {
                Log.d("ControlLight", "Response received for Light $lightNumber: ${response.body()}")
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.d("ControlLight", "Response failed with status: ${response.code()}")
                    onResult(false)
                }
            }
            override fun onFailure(call: Call<ControlResponse>, t: Throwable) {
                Log.e("ControlLight", "Request failed for Light $lightNumber: ${t.message}")
                onResult(false)
            }
        })
    } else if(lightNumber == 2) {
        RetrofitInstance.lightApi.controlLight_2(request).enqueue(object : Callback<ControlResponse> {
            override fun onResponse(call: Call<ControlResponse>, response: Response<ControlResponse>) {
                Log.d("ControlLight", "Response received for Light $lightNumber: ${response.body()}")
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.d("ControlLight", "Response failed with status: ${response.code()}")
                    onResult(false)
                }
            }
            override fun onFailure(call: Call<ControlResponse>, t: Throwable) {
                Log.e("ControlLight", "Request failed for Light $lightNumber: ${t.message}")
                onResult(false)
            }
        })
    }
}


@Composable
fun AirConditionerControl(onBack: () -> Unit) {
    var isAirConditionerOn by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("Cooling") } // Modes: Cooling, Heating, Dehumidifying
    var temperature by remember { mutableStateOf(26) } // Initial temperature set to 26°C
    var fanSpeed by remember { mutableIntStateOf(0) } // 0: Auto, 1: Low, 2: Medium, 3: High
    var hoursInput by remember { mutableStateOf("0") }
    var minutesInput by remember { mutableStateOf("0") }
    var secondsInput by remember { mutableStateOf("0") }
    var isTimerRunning by remember { mutableStateOf(false) }
    var timeLeft by remember { mutableStateOf(0) } // Time left in seconds
    // Create a scroll state
    val scrollState = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState) // Add scrolling
    ){
        Text("Air Conditioner Control", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Toggle air conditioner on/off
        Button(
            onClick = {
                if (timeLeft == 0) {
                    isAirConditionerOn = !isAirConditionerOn
                }
                GlobalVariable.Change_AirConditioner_state()
                AirConditioner_state = GlobalVariable.Get_AirConditioner_state()
                controlAirConditionerWrapper(AirConditioner_state,1){success ->
                    if (success) {
                        Log.d("Control_AirConditioner", "冷氣已開啟")
                    } else {
                        GlobalVariable.Change_AirConditioner_state()
                        AirConditioner_state = GlobalVariable.Get_Fan_state_1()
                        Log.d("Control_AirConditioner", "冷氣開啟失敗")
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (AirConditioner_state) Color.Green else Color.Red
            )
        ) {
            Text(text = if (AirConditioner_state) "Turn Off" else "Turn On")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode selection
        Text(text = "Mode", fontSize = 20.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton("Cooling", mode, isAirConditionerOn) { mode = it }
           /* ModeButton("Heating", mode, isAirConditionerOn) { mode = it }*/
            ModeButton("Dehumidifying", mode, isAirConditionerOn) { mode = it }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Temperature control
        Text(text = "Temperature", fontSize = 20.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { if (temperature > 16) temperature-- }, enabled = isAirConditionerOn && mode == "Cooling") {
                Text("-")
            }
            Text(text = "${temperature}°C", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { if (temperature < 30) temperature++ }, enabled = isAirConditionerOn && mode == "Cooling") {
                Text("+")
            }
        }

//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Fan speed control
//        Text(text = "Fan Speed", fontSize = 20.sp)
//        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//           /* FanSpeedButton("Auto", 0, fanSpeed, isAirConditionerOn && mode == "Cooling") { fanSpeed = it }*/
//            FanSpeedButton("Low", 1, fanSpeed, isAirConditionerOn && mode == "Cooling") { fanSpeed = it }
//            FanSpeedButton("Medium", 2, fanSpeed, isAirConditionerOn && mode == "Cooling") { fanSpeed = it }
//            FanSpeedButton("High", 3, fanSpeed, isAirConditionerOn && mode == "Cooling") { fanSpeed = it }
//        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer input fields
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Timer", fontSize = 16.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Hours")
                    TextField(
                        value = if (isTimerRunning) "%02d".format(timeLeft / 3600) else hoursInput,
                        onValueChange = { hoursInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.width(80.dp),
                        enabled = !isTimerRunning
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Minutes")
                    TextField(
                        value = if (isTimerRunning) "%02d".format((timeLeft / 60) % 60) else minutesInput,
                        onValueChange = { minutesInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.width(80.dp),
                        enabled = !isTimerRunning
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Seconds")
                    TextField(
                        value = if (isTimerRunning) "%02d".format(timeLeft % 60) else secondsInput,
                        onValueChange = { secondsInput = it.filter { char -> char.isDigit() } },
                        modifier = Modifier.width(80.dp),
                        enabled = !isTimerRunning
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Start Timer button
        Button(
            onClick = {
                val totalSeconds = (hoursInput.toIntOrNull() ?: 0) * 3600 +
                        (minutesInput.toIntOrNull() ?: 0) * 60 +
                        (secondsInput.toIntOrNull() ?: 0)

                if (totalSeconds > 0) {
                    timeLeft = totalSeconds
                    isTimerRunning = true
                    isAirConditionerOn = true // 啟動計時器時自動開啟冷氣
                    GlobalVariable.Change_AirConditioner_state()
                    AirConditioner_state = true

                    controlAirConditionerWrapper(true, 1) { success ->
                        if (success) {
                            Log.d("Control_AirConditioner", "冷氣計時啟動成功")
                        } else {
                            // 如果啟動失敗，恢復狀態
                            GlobalVariable.Change_AirConditioner_state()
                            isAirConditionerOn = false
                            AirConditioner_state = false
                            Log.d("Control_AirConditioner", "冷氣計時啟動失敗")
                        }
                    }
                } else {
                    Log.d("Control_AirConditioner", "無效的計時器輸入")
                }
            }
        ) {
            Text("Start")
        }


        Spacer(modifier = Modifier.height(4.dp))

        Button(onClick = onBack) {
            Text("Back")
        }
    }

    // Simulate the timer countdown
    LaunchedEffect(isTimerRunning, timeLeft) {
        if (isTimerRunning && timeLeft > 0) {
            kotlinx.coroutines.delay(1000L) // 每秒倒數
            timeLeft--
        } else if (timeLeft == 0 && isTimerRunning) {
            isTimerRunning = false
            isAirConditionerOn = false
            GlobalVariable.Change_AirConditioner_state()
            AirConditioner_state = false

            controlAirConditionerWrapper(false, 1) { success ->
                if (success) {
                    Log.d("Control_AirConditioner", "計時結束，冷氣已自動關閉")
                } else {
                    Log.d("Control_AirConditioner", "計時結束但冷氣關閉失敗")
                }
            }
        }
    }

}
private fun controlAirConditionerWrapper(status: Boolean, Number: Int, onResult: (Boolean) -> Unit) {
    controlAirConditioner(status, Number, onResult)
}

private fun controlAirConditioner(status: Boolean, Number: Int, onResult: (Boolean) -> Unit) {
    val request = ControlRequest(status)
    if (Number == 1) {
        RetrofitInstance.lightApi.controlAirConditioner(request).enqueue(object : Callback<ControlResponse> {
            override fun onResponse(call: Call<ControlResponse>, response: Response<ControlResponse>) {
                Log.d("controlAirConditioner", "Response received for Airconditioner $Number: ${response.body()}")
                if (response.isSuccessful) {
                    onResult(true)
                } else {
                    Log.d("controlAirConditioner", "Response failed with status: ${response.code()}")
                    onResult(false)
                }
            }
            override fun onFailure(call: Call<ControlResponse>, t: Throwable) {
                Log.e("controlAirConditioner", "Request failed for Airconditioner $Number: ${t.message}")
                onResult(false)
            }
        })
    }
}
// Mode button composable
@Composable
fun ModeButton(label: String, currentMode: String, isEnabled: Boolean, onModeChange: (String) -> Unit) {
    Button(
        onClick = { onModeChange(label) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (currentMode == label) Color.DarkGray else Color.LightGray
        ),
        enabled = isEnabled
    ) {
        Text(label)
    }
}

// Fan speed button composable
@Composable
fun FanSpeedButton(label: String, speed: Int, currentSpeed: Int, isEnabled: Boolean, onSpeedChange: (Int) -> Unit) {
    Button(
        onClick = { onSpeedChange(speed) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (currentSpeed == speed) Color.DarkGray else Color.LightGray
        ),
        enabled = isEnabled
    ) {
        Text(label)
    }
}

enum class ControlType {
    Fan, Light, AirConditioner
}