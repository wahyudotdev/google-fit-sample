package com.example.googlefit

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.googlefit.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataPoint
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.HealthDataTypes
import com.google.android.gms.fitness.data.HealthDataTypes.TYPE_BLOOD_PRESSURE
import com.google.android.gms.fitness.data.HealthFields.*
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 100
        private val TAG = MainActivity::class.simpleName
    }

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val fitnessOptions by lazy {
        FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ) // steps
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ) // steps
            .addDataType(HealthDataTypes.AGGREGATE_BLOOD_PRESSURE_SUMMARY, FitnessOptions.ACCESS_READ) // blood pressure
            .addDataType(HealthDataTypes.AGGREGATE_BLOOD_PRESSURE_SUMMARY, FitnessOptions.ACCESS_WRITE) // blood pressure
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ) // bpm
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_WRITE) // bpm
            .addDataType(HealthDataTypes.TYPE_OXYGEN_SATURATION, FitnessOptions.ACCESS_WRITE) // oxygen
            .addDataType(HealthDataTypes.TYPE_OXYGEN_SATURATION, FitnessOptions.ACCESS_READ) // oxygen
            .build()
    }

    private val account by lazy {
        GoogleSignIn.getAccountForExtension(baseContext, fitnessOptions)
    }

    private var systolic: Float? = null
    private var diastolic: Float? = null
    private var bpm: Float? = null
    private var oxygen: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.button.setOnClickListener {
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    this,
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions
                )
            } else {
                getGoogleFitData()
            }
        }
        binding.etSystolic.editText?.doOnTextChanged { text, start, before, count ->
            systolic = text.toString().toFloatOrNull()
        }
        binding.etDiastolic.editText?.doOnTextChanged { text, start, before, count ->
            diastolic = text.toString().toFloatOrNull()
        }
        binding.etBpm.editText?.doOnTextChanged { text, start, before, count ->
            bpm = text.toString().toFloatOrNull()
        }
        binding.etOxygen.editText?.doOnTextChanged { text, start, before, count ->
            oxygen = text.toString().toFloatOrNull()
        }
        binding.btnRecord.setOnClickListener {
            val bloodPressureSource = DataSource.Builder()
                .setDataType(TYPE_BLOOD_PRESSURE)
                .setAppPackageName(this)
                .setType(DataSource.TYPE_RAW)
                .build()


            if (Build.VERSION.SDK_INT >= 26) {
                val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val startTime = endTime.minusHours(1)
                val bloodPressure = DataPoint.builder(bloodPressureSource)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setField(FIELD_BLOOD_PRESSURE_SYSTOLIC, systolic ?: 0f)
                    .setField(FIELD_BLOOD_PRESSURE_DIASTOLIC, diastolic ?: 0f)
                    .setField(FIELD_BODY_POSITION, BODY_POSITION_SITTING)
                    .setField(
                        FIELD_BLOOD_PRESSURE_MEASUREMENT_LOCATION,
                        BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_UPPER_ARM
                    )
                    .setTimeInterval(
                        startTime.toEpochSecond(),
                        endTime.toEpochSecond(),
                        TimeUnit.SECONDS
                    )

                    .build()

                val dataSet = DataSet.builder(bloodPressureSource)
                    .add(bloodPressure)
                    .build()

                Fitness.getHistoryClient(
                    this,
                    GoogleSignIn.getAccountForExtension(this, fitnessOptions)
                )
                    .insertData(dataSet)
                    .addOnSuccessListener {
                        Log.d(TAG, "onCreate: succes")

                    }.addOnFailureListener {
                        Log.e(TAG, "onCreate: fail", it)
                    }
            }
        }


        binding.btnRecordBpm.setOnClickListener {

            if (Build.VERSION.SDK_INT >= 26) {
                val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val startTime = endTime.minusHours(1)
                val dataSource = DataSource.Builder()
                    .setDataType(DataType.TYPE_HEART_RATE_BPM)
                    .setAppPackageName(this)
                    .setType(DataSource.TYPE_RAW)
                    .build()
                val dataPoint = DataPoint.builder(dataSource)
                    .setField(Field.FIELD_BPM, bpm ?: 0f)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
                val dataSet = DataSet.builder(dataSource)
                    .add(dataPoint)
                    .build()
                Fitness.getHistoryClient(
                    this,
                    GoogleSignIn.getAccountForExtension(this, fitnessOptions)
                )
                    .insertData(dataSet)
                    .addOnSuccessListener {
                        Log.d(TAG, "onCreate: bpm succes")

                    }.addOnFailureListener {
                        Log.e(TAG, "onCreate: bpm fail", it)
                    }
            }

        }

        binding.btnRecodOxygen.setOnClickListener {

            if (Build.VERSION.SDK_INT >= 26) {
                val endTime = LocalDateTime.now().atZone(ZoneId.systemDefault())
                val startTime = endTime.minusHours(1)
                val dataSource = DataSource.Builder()
                    .setDataType(HealthDataTypes.TYPE_OXYGEN_SATURATION)
                    .setAppPackageName(this)
                    .setType(DataSource.TYPE_RAW)
                    .build()
                val dataPoint = DataPoint.builder(dataSource)
                    .setField(FIELD_OXYGEN_SATURATION, oxygen ?: 0f)
                    .setField(FIELD_SUPPLEMENTAL_OXYGEN_FLOW_RATE, 0f)
                    .setTimestamp(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                    .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                    .build()
                val dataSet = DataSet.builder(dataSource)
                    .add(dataPoint)
                    .build()
                Fitness.getHistoryClient(
                    this,
                    GoogleSignIn.getAccountForExtension(this, fitnessOptions)
                )
                    .insertData(dataSet)
                    .addOnSuccessListener {
                        Log.d(TAG, "onCreate: bpm succes")

                    }.addOnFailureListener {
                        Log.e(TAG, "onCreate: bpm fail", it)
                    }
            }

        }
    }

    private fun getGoogleFitData() {
        Fitness.getHistoryClient(
            this,
            GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        ).readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { result ->
                val totalSteps =
                    result.dataPoints.firstOrNull()?.getValue(Field.FIELD_STEPS)?.asInt() ?: 0
                binding.tvSteps.text = getString(R.string.steps_count, totalSteps.toString())
            }
            .addOnFailureListener {
                Log.w(TAG, "failed to get step data with error ", it)
            }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> when (requestCode) {
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> {
                    getGoogleFitData()
                    Toast.makeText(
                        this,
                        "Permission granted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else -> {
                    Log.d("TAG", "onActivityResult: Result wasn't from Google Fit")
                }
            }
            else -> {
                Log.d("TAG", "onActivityResult: Permission denied")
                Toast.makeText(
                    this,
                    "Permission denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}