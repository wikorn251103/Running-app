package com.example.myproject.Fragment.training

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.data.training.TrainingModel
import com.example.myproject.data.training.TrainingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch
import java.util.Calendar

class TrainingScheduleViewModel(private val repository: TrainingRepository) : ViewModel() {

    private val _trainingDays = MutableLiveData<List<TrainingModel>>()
    val trainingDays: LiveData<List<TrainingModel>> get() = _trainingDays

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    // ⭐ เพิ่ม LiveData สำหรับสัปดาห์ปัจจุบัน
    private val _currentWeek = MutableLiveData<Int>()
    val currentWeek: LiveData<Int> get() = _currentWeek

    var selectedTrainingPlanId: String? = null

    private var weekListener: ListenerRegistration? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "TrainingScheduleVM"
    }

    /**
     * ⭐ โหลดข้อมูลแบบ Real-time
     */
    fun loadTrainingWeekRealtime(week: Int) {
        _loading.value = true
        _error.value = null
        _currentWeek.value = week

        Log.d(TAG, "📡 Loading week $week with real-time updates")

        // ยกเลิก listener เก่า
        weekListener?.remove()

        // สร้าง listener ใหม่
        weekListener = repository.getTrainingWeekDataRealtime(week,
            onSuccess = { days ->
                _loading.value = false
                _trainingDays.value = days
                Log.d(TAG, "✅ Received ${days.size} days for week $week")
            },
            onFailure = { exception ->
                _loading.value = false
                _error.value = exception.message
                Log.e(TAG, "❌ Error loading week $week: ${exception.message}")
            }
        )
    }

    /**
     * ⭐ คำนวณสัปดาห์ปัจจุบันจาก Firebase
     */
    fun calculateAndLoadCurrentWeek() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "❌ User not logged in")
            _currentWeek.value = 1
            loadTrainingWeekRealtime(1)
            return
        }

        Log.d(TAG, "🔄 Calculating current week for user: $userId")

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val startDate = document.getTimestamp("startDate")
                    val calculatedWeek = calculateWeekFromStartDate(startDate?.toDate()?.time ?: 0L)

                    Log.d(TAG, "📅 Calculated current week: $calculatedWeek")

                    _currentWeek.value = calculatedWeek
                    loadTrainingWeekRealtime(calculatedWeek)
                } else {
                    Log.w(TAG, "⚠️ No Athletes document found, defaulting to week 1")
                    _currentWeek.value = 1
                    loadTrainingWeekRealtime(1)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Failed to fetch Athletes document: ${e.message}", e)
                _currentWeek.value = 1
                loadTrainingWeekRealtime(1)
            }
    }

    /**
     * ⭐ คำนวณสัปดาห์จากวันที่เริ่มโปรแกรม
     */
    private fun calculateWeekFromStartDate(startDateMillis: Long): Int {
        if (startDateMillis == 0L) {
            Log.w(TAG, "⚠️ No start date, defaulting to week 1")
            return 1
        }

        val startCalendar = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val daysDiff = ((today.timeInMillis - startCalendar.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        val currentWeek = (daysDiff / 7) + 1

        Log.d(TAG, "📊 Days since start: $daysDiff, Calculated week: $currentWeek")

        // จำกัดไม่ให้น้อยกว่า 1 และไม่เกิน 12 สัปดาห์
        return currentWeek.coerceIn(1, 12)
    }

    /**
     * ⭐ ตรวจสอบวันที่ขาดซ้อม (เช็ควันที่ผ่านไปแล้ว)
     */
    fun checkMissedDays(week: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            firestore.collection("Athletes")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val weekData = document.get("week_$week") as? HashMap<*, *> ?: return@addOnSuccessListener

                        val today = Calendar.getInstance()
                        today.set(Calendar.HOUR_OF_DAY, 0)
                        today.set(Calendar.MINUTE, 0)
                        today.set(Calendar.SECOND, 0)
                        today.set(Calendar.MILLISECOND, 0)

                        val programStartDate = document.getTimestamp("startDate")?.toDate()

                        if (programStartDate != null) {
                            val programStart = Calendar.getInstance().apply {
                                time = programStartDate
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }

                            for (i in 1..7) {
                                val dayData = weekData["day_$i"] as? HashMap<*, *> ?: continue

                                val isCompleted = dayData["isCompleted"] as? Boolean ?: false
                                val isMissed = dayData["isMissed"] as? Boolean ?: false
                                val type = dayData["type"] as? String ?: ""

                                // คำนวณวันที่ของ day นั้นๆ
                                val dayDate = Calendar.getInstance().apply {
                                    time = programStart.time
                                    add(Calendar.DAY_OF_YEAR, ((week - 1) * 7) + (i - 1))
                                }

                                // ถ้าวันนั้นผ่านไปแล้ว และไม่ได้ซ้อม และไม่ใช่ Rest Day
                                if (dayDate.before(today) &&
                                    !isCompleted &&
                                    !isMissed &&
                                    !type.equals("Rest Day", ignoreCase = true)) {

                                    // Mark as missed
                                    val fieldPath = "week_$week.day_$i.isMissed"
                                    firestore.collection("Athletes")
                                        .document(userId)
                                        .update(fieldPath, true)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "✅ Marked week $week day $i as missed")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "❌ Failed to mark day as missed", e)
                                        }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "❌ Error checking missed days", e)
                }
        }
    }

    /**
     * ⭐ ตรวจสอบว่ามีการซ้อมที่ค้างอยู่หรือไม่
     */
    fun checkPendingWorkouts(callback: (hasPending: Boolean, pendingWeek: Int?) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(false, null)
            return
        }

        firestore.collection("Athletes")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val startDate = document.getTimestamp("startDate")
                    val currentWeek = calculateWeekFromStartDate(startDate?.toDate()?.time ?: 0L)

                    // ตรวจสอบว่ามีการซ้อมที่ยังไม่เสร็จในสัปดาห์ปัจจุบันหรือไม่
                    val weekData = document.get("week_$currentWeek") as? HashMap<*, *>

                    var hasPending = false
                    if (weekData != null) {
                        for (i in 1..7) {
                            val dayData = weekData["day_$i"] as? HashMap<*, *> ?: continue
                            val isCompleted = dayData["isCompleted"] as? Boolean ?: false
                            val type = dayData["type"] as? String ?: ""

                            // ถ้ายังไม่ได้ซ้อมและไม่ใช่ Rest Day
                            if (!isCompleted && !type.equals("Rest Day", ignoreCase = true)) {
                                hasPending = true
                                break
                            }
                        }
                    }

                    callback(hasPending, if (hasPending) currentWeek else null)
                    Log.d(TAG, "✅ Checked pending workouts: hasPending=$hasPending, week=$currentWeek")
                } else {
                    callback(false, null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error checking pending workouts", e)
                callback(false, null)
            }
    }

    /**
     * เก่า - โหลดแบบปกติ (ไม่ real-time)
     */
    fun loadTrainingWeek(planId: String, week: Int) {
        _loading.value = true
        _error.value = null
        _currentWeek.value = week

        repository.getTrainingWeekData(planId, week,
            onSuccess = { days ->
                _loading.value = false
                _trainingDays.value = days
                Log.d(TAG, "✅ Loaded ${days.size} days for week $week")
            },
            onFailure = { exception ->
                _loading.value = false
                _error.value = exception.message
                Log.e(TAG, "❌ Error loading week $week: ${exception.message}")
            }
        )
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // ยกเลิก listener เมื่อ ViewModel ถูกทำลาย
        weekListener?.remove()
        repository.removeListener()
        Log.d(TAG, "🔕 Listeners cleaned up")
    }
}