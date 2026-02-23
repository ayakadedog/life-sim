const { ref, reactive, onMounted } = Vue;

const app = Vue.createApp({
    setup() {
        // App State
        const step = ref(0); // 0=Login, 1=Init, 2=Probes, 3=Game
        const currentTab = ref('start'); // start, history, mine
        const loading = ref(false);
        const loadingText = ref('加载中...');
        
        // User Data
        const loginPhone = ref('');
        const user = ref(null);
        const historyList = ref([]);
        const templates = ref([]);
        
        // Game State
        const probes = ref([]);
        const currentProbeIndex = ref(0);
        const probeAnswers = reactive({});
        
        const currentProfile = ref(null);
        const skipValue = ref(3);
        
        // Typewriter effect variables
        const displayedEvent = ref('');
        const isTyping = ref(false);
        
        const profile = reactive({
            basicInfo: { name: '张三', startAge: 25, location: '一线城市 (北上广深)', educationLevel: 'Bachelor', profession: '程序员', lifeExperiences: '' },
            economicStatus: { savings: 50000, debt: 0 },
            healthStatus: { energyLevel: 80 },
            familyBackground: { 
                parentsStatus: 'Parents Alive',
                familyAssets: '',
                fatherProfession: '',
                motherProfession: ''
            },
            difficulty: 'Normal'
        });

        // --- Persistence Logic ---
        onMounted(() => {
            const savedUser = localStorage.getItem('lifeSimUser');
            if (savedUser) {
                user.value = JSON.parse(savedUser);
                step.value = 1; // Skip login
                loadUserData();
            }
        });

        // --- Methods ---

        const setLoading = (status, text = '加载中...') => {
            loading.value = status;
            loadingText.value = text;
        };

        const login = async () => {
            if (!loginPhone.value) return alert('请输入手机号');
            setLoading(true, '登录中...');
            try {
                const res = await fetch('/api/v1/user/login', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ phone: loginPhone.value })
                });
                if (!res.ok) throw new Error('登录失败');
                user.value = await res.json();
                
                // Save to localStorage
                localStorage.setItem('lifeSimUser', JSON.stringify(user.value));
                
                step.value = 1; // Go to Init
                loadUserData();
            } catch (e) {
                alert(e.message);
            } finally {
                setLoading(false);
            }
        };
        
        const logout = () => {
            localStorage.removeItem('lifeSimUser');
            user.value = null;
            step.value = 0;
            currentTab.value = 'start';
        };

        const loadUserData = async () => {
            if (!user.value) return;
            // Load History
            try {
                const resHist = await fetch(`/api/v1/user/${user.value.id}/history`);
                if (resHist.ok) {
                    const data = await resHist.json();
                    console.log("History data:", data);
                    historyList.value = data;
                } else {
                    console.error("Failed to load history:", resHist.status);
                }
            } catch(e) {
                console.error("Error loading history:", e);
            }
            
            // Load Templates
            try {
                const resTemp = await fetch(`/api/v1/user/${user.value.id}/templates`);
                if (resTemp.ok) {
                    const data = await resTemp.json();
                    console.log("Templates data:", data);
                    templates.value = data;
                } else {
                    console.error("Failed to load templates:", resTemp.status);
                }
            } catch(e) {
                console.error("Error loading templates:", e);
            }
        };
        
        const switchTab = (tab) => {
            currentTab.value = tab;
            if (tab === 'history' || tab === 'mine') {
                loadUserData();
            }
        };

        const updateProfileAndType = (newProfile) => {
            console.log("🔥 [DEBUG] Received Profile:", newProfile);
            currentProfile.value = newProfile;
            
            // Check if scenario is undefined
            if (newProfile.currentScenario === undefined) {
                console.error("❌ [ERROR] currentScenario is UNDEFINED");
                displayedEvent.value = "数据加载失败，请刷新重试";
                return;
            }
            
            console.log("📝 [DEBUG] Raw Scenario Type:", typeof newProfile.currentScenario);
            console.log("📝 [DEBUG] Raw Scenario Value:", newProfile.currentScenario);
            
            const scenario = parseScenario(newProfile.currentScenario);
            console.log("✅ [DEBUG] Parsed Scenario:", scenario);
            
            // Ensure event exists
            const eventText = scenario.event || "无内容";
            console.log("📜 [DEBUG] Event Text:", eventText);
            
            displayedEvent.value = typeof eventText === 'string' ? eventText : JSON.stringify(eventText);
        };

        const initProbes = async () => {
            setLoading(true, '正在分析你的人生档案...');
            try {
                // 1. Create Initial Profile (Transient)
                const res = await fetch('/api/v1/sim/init', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(profile)
                });
                if (!res.ok) throw new Error('网络请求失败: ' + res.status);
                const savedProfile = await res.json();
                profile.id = savedProfile.id; // Update ID
                
                // 2. Get Probes
                const resProbes = await fetch('/api/v1/sim/probes', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(profile)
                });
                if (!resProbes.ok) throw new Error('网络请求失败: ' + resProbes.status);
                probes.value = await resProbes.json();
                step.value = 2;
            } catch (e) {
                alert('发生错误: ' + e.message);
                console.error(e);
            } finally {
                setLoading(false);
            }
        };

        const startSimulation = async () => {
            setLoading(true, '正在生成你的平行宇宙...');
            try {
                // 1. Auto-save template
                await fetch(`/api/v1/template/create?userId=${user.value.id}`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(profile)
                });
                
                // 2. Start Game Instance
                const gameRes = await fetch(`/api/v1/game/start?userId=${user.value.id}`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(profile) 
                });
                const gameInstance = await gameRes.json();
                const profileId = gameInstance.userProfile.id;

                // 防呆处理：如果有问题未回答，自动填充默认值，防止后端LLM处理异常
                if (probes.value && probes.value.length > 0) {
                    probes.value.forEach(probe => {
                        if (!probeAnswers[probe] || !probeAnswers[probe].trim()) {
                            probeAnswers[probe] = "（用户选择保持沉默，没有回答）";
                        }
                    });
                }

                // 3. Analyze Answers & Start Loop
                const res = await fetch(`/api/v1/sim/${profileId}/start`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(probeAnswers)
                });
                if (!res.ok) throw new Error('网络请求失败: ' + res.status);
                updateProfileAndType(await res.json());
                step.value = 3;

                // 4. Refresh User Data (History & Templates)
                loadUserData();
            } catch (e) {
                alert('发生错误: ' + e.message);
                console.error(e);
            } finally {
                setLoading(false);
            }
        };
        
        const continueGame = (instance) => {
            currentProfile.value = instance.userProfile;
            profile.id = instance.userProfile.id; // Sync ID
            updateProfileAndType(instance.userProfile);
            step.value = 3;
            currentTab.value = 'start';
            // Also refresh data to ensure we have latest status
            loadUserData();
        };
        
        const useTemplate = (template) => {
            try {
                // Parse JSON strings safely
                const basicInfo = typeof template.basicInfo === 'string' ? JSON.parse(template.basicInfo) : template.basicInfo;
                const familyBackground = typeof template.familyBackground === 'string' ? JSON.parse(template.familyBackground) : template.familyBackground;
                const economicStatus = typeof template.initialAttributes === 'string' ? JSON.parse(template.initialAttributes) : template.initialAttributes;
                
                Object.assign(profile.basicInfo, basicInfo);
                Object.assign(profile.familyBackground, familyBackground);
                if(economicStatus) Object.assign(profile.economicStatus, economicStatus);
                
                step.value = 1;
                currentTab.value = 'start';
                currentProbeIndex.value = 0;
            } catch(e) {
                console.error("Template parse error", e);
                alert("模板数据损坏，无法加载");
            }
        };

        const parseScenario = (scenario) => {
            if (!scenario) return { event: "无事发生", status_change: "无明显变化", relationship_change: "一切如常" };
            
            try {
                let parsed = scenario;
                // Try parsing string to object
                if (typeof parsed === 'string') {
                    try {
                        parsed = JSON.parse(parsed);
                    } catch (e) {
                        // Not JSON, just a string
                        return { event: parsed, status_change: "无明显变化", relationship_change: "一切如常" };
                    }
                }
                
                // If it's still a string (double encoded), parse again
                if (typeof parsed === 'string') {
                    try {
                        parsed = JSON.parse(parsed);
                    } catch (e) {
                        // Still a string, treat as event text
                        return { event: parsed, status_change: "无明显变化", relationship_change: "一切如常" };
                    }
                }

                // If result is an object, normalize keys
                if (parsed && typeof parsed === 'object') {
                    return {
                        event: parsed.event || parsed.message || JSON.stringify(parsed),
                        status_change: parsed.status_change || "无明显变化",
                        relationship_change: parsed.relationship_change || "一切如常"
                    };
                }
                
                // Fallback for weird types
                return { event: String(parsed), status_change: "无明显变化", relationship_change: "一切如常" };

            } catch (e) {
                console.error("Critical Parse Error:", e);
                return {
                    event: "数据解析错误: " + e.message,
                    status_change: "无明显变化",
                    relationship_change: "一切如常"
                };
            }
        };

        const nextYear = async (choice) => {
            if (!choice) return;
            setLoading(true, '时光流转中...');
            try {
                const res = await fetch(`/api/v1/sim/${currentProfile.value.id}/next`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ choice })
                });
                if (!res.ok) throw new Error('网络请求失败: ' + res.status);
                const data = await res.json();
                console.log("Next Year Response:", data);
                updateProfileAndType(data);
            } catch (e) {
                alert('发生错误: ' + e.message);
                console.error(e);
            } finally {
                setLoading(false);
            }
        };
        
        const skipYears = async () => {
            if(!confirm(`确定要快速跳过 ${skipValue.value} 年吗？这期间将无法进行精细操作。`)) return;
            setLoading(true, `正在快进 ${skipValue.value} 年...`);
            try {
                const res = await fetch(`/api/v1/sim/${currentProfile.value.id}/skip?years=${skipValue.value}`, {
                    method: 'POST'
                });
                if (!res.ok) throw new Error('网络请求失败: ' + res.status);
                updateProfileAndType(await res.json());
            } catch (e) {
                alert('发生错误: ' + e.message);
                console.error(e);
            } finally {
                setLoading(false);
            }
        };
        
        const createLegacy = async () => {
            setLoading(true, '正在传承意志...');
            try {
                const res = await fetch(`/api/v1/sim/${currentProfile.value.id}/legacy`, {
                    method: 'POST'
                });
                if (!res.ok) throw new Error('网络请求失败: ' + res.status);
                updateProfileAndType(await res.json());
                alert("你已继承家业，开始下一代的人生！");
            } catch (e) {
                alert('发生错误: ' + e.message);
                console.error(e);
            } finally {
                setLoading(false);
            }
        };

        const safeParse = (str) => {
            if (!str) return {};
            try {
                return typeof str === 'string' ? JSON.parse(str) : str;
            } catch (e) {
                console.error("JSON parse error:", e, str);
                return {};
            }
        };

        const getScenarioEvent = (scenarioStr) => {
            const scenario = parseScenario(scenarioStr);
            return scenario.event;
        };

        return {
            step, loading, loadingText, currentTab,
            loginPhone, user, historyList, templates,
            profile, probes, currentProbeIndex, probeAnswers,
            currentProfile, skipValue,
            login, logout, switchTab, continueGame, useTemplate,
            initProbes, startSimulation, nextYear, skipYears, createLegacy,
            parseScenario, displayedEvent, isTyping, safeParse, getScenarioEvent
        };
    }
});

app.mount('#app');
