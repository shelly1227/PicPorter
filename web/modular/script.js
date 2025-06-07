// 全局变量
const API_BASE = 'http://localhost:8080';
const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB分片大小

let fileQueue = []; // 上传队列
let currentPage = 1;
let pageSize = 20;
let serverFiles = [];
let filteredServerFiles = [];

// DOM 元素
const uploadArea = document.getElementById('uploadArea');
const fileInput = document.getElementById('fileInput');
const folderInput = document.getElementById('folderInput');
const selectFilesBtn = document.getElementById('selectFilesBtn');
const selectFolderBtn = document.getElementById('selectFolderBtn');
const fileList = document.getElementById('fileList');
const articleTitle = document.getElementById('articleTitle');
const articleContent = document.getElementById('articleContent');
const processArticleBtn = document.getElementById('processArticleBtn');
const serverFileList = document.getElementById('serverFileList');
const loadingOverlay = document.getElementById('loadingOverlay');
const notificationContainer = document.getElementById('notificationContainer');

// 初始化
document.addEventListener('DOMContentLoaded', function() {
    initEventListeners();
    loadServerFiles();
    updateStats();
});

// 初始化事件监听器
function initEventListeners() {
    // 上传区域事件
    uploadArea.addEventListener('click', () => fileInput.click());
    uploadArea.addEventListener('dragover', handleDragOver);
    uploadArea.addEventListener('dragleave', handleDragLeave);
    uploadArea.addEventListener('drop', handleDrop);

    // 按钮事件
    selectFilesBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        fileInput.click();
    });

    selectFolderBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        folderInput.click();
    });

    // 文件输入事件
    fileInput.addEventListener('change', (e) => handleFileSelect(e.target.files));
    folderInput.addEventListener('change', (e) => handleFileSelect(e.target.files));

    // 文章处理
    processArticleBtn.addEventListener('click', handleArticleProcess);

    // 队列控制按钮
    document.getElementById('clearAllBtn').addEventListener('click', clearAllFiles);
    document.getElementById('pauseAllBtn').addEventListener('click', pauseAllUploads);
    document.getElementById('startAllBtn').addEventListener('click', startAllUploads);

    // 服务器文件刷新
    document.getElementById('refreshServerFilesBtn').addEventListener('click', loadServerFiles);

    // 过滤器
    document.getElementById('statusFilter').addEventListener('change', filterFiles);
    document.getElementById('typeFilter').addEventListener('change', filterFiles);
    document.getElementById('searchInput').addEventListener('input', filterFiles);

    // 分页控制
    document.getElementById('pageSizeSelect').addEventListener('change', function() {
        pageSize = parseInt(this.value);
        currentPage = 1;
        loadServerFiles();
    });

    // 模态框关闭
    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', function() {
            this.closest('.modal').classList.remove('show');
        });
    });

    // 确认模态框
    document.getElementById('confirmCancel').addEventListener('click', function() {
        document.getElementById('confirmModal').classList.remove('show');
    });
}

// 拖拽处理
function handleDragOver(e) {
    e.preventDefault();
    uploadArea.classList.add('dragover');
}

function handleDragLeave(e) {
    e.preventDefault();
    uploadArea.classList.remove('dragover');
}

function handleDrop(e) {
    e.preventDefault();
    uploadArea.classList.remove('dragover');

    const files = Array.from(e.dataTransfer.files);
    handleFileSelect(files);
}

// 文件选择处理
function handleFileSelect(files) {
    if (!files || files.length === 0) return;

    Array.from(files).forEach(file => {
        const fileObj = {
            id: generateFileId(),
            file: file,
            name: file.name,
            size: file.size,
            type: getFileType(file.type),
            status: 'waiting',
            progress: 0,
            identifier: null,
            uploadId: null,
            chunkNum: 0,
            uploadedChunks: [],
            error: null
        };

        fileQueue.push(fileObj);
    });

    renderFileList();
    updateStats();

    // 自动开始上传
    startAllUploads();
}

// 生成文件ID
function generateFileId() {
    return Date.now() + '-' + Math.random().toString(36).substr(2, 9);
}

// 获取文件类型
function getFileType(mimeType) {
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType.startsWith('video/')) return 'video';
    if (mimeType.includes('pdf') || mimeType.includes('document') || mimeType.includes('text')) return 'document';
    return 'other';
}

// 计算文件MD5
async function calculateMD5(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = function(e) {
            const arrayBuffer = e.target.result;
            const hash = CryptoJS.MD5(CryptoJS.lib.WordArray.create(arrayBuffer));
            resolve(hash.toString());
        };
        reader.onerror = reject;
        reader.readAsArrayBuffer(file);
    });
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 获取文件图标
function getFileIcon(type) {
    const icons = {
        image: 'fa-image',
        video: 'fa-video',
        document: 'fa-file-text',
        other: 'fa-file'
    };
    return icons[type] || 'fa-file';
}

// 渲染文件列表
function renderFileList() {
    const filteredFiles = getFilteredFiles();

    if (filteredFiles.length === 0) {
        fileList.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-inbox"></i>
                <h3>暂无文件</h3>
                <p>拖拽或选择文件开始上传</p>
            </div>
        `;
        return;
    }

    const html = filteredFiles.map(file => `
        <div class="file-item ${file.status}" data-file-id="${file.id}">
            <div class="file-header">
                <div class="file-info">
                    <div class="file-name">
                        <i class="file-icon fas ${getFileIcon(file.type)}"></i>
                        ${file.name}
                    </div>
                    <div class="file-details">
                        <div class="file-size">
                            <i class="fas fa-hdd"></i>
                            ${formatFileSize(file.size)}
                        </div>
                        <div class="file-type">
                            <i class="fas fa-tag"></i>
                            ${file.type}
                        </div>
                        <div class="file-status">
                            <i class="fas ${getStatusIcon(file.status)}"></i>
                            ${getStatusText(file.status)}
                        </div>
                    </div>
                </div>
                <div class="file-actions">
                    ${file.status === 'uploading' ?
        `<button class="btn btn-warning btn-small" onclick="pauseUpload('${file.id}')">
                            <i class="fas fa-pause"></i>
                        </button>` :
        file.status === 'waiting' || file.status === 'paused' ?
            `<button class="btn btn-success btn-small" onclick="startUpload('${file.id}')">
                            <i class="fas fa-play"></i>
                        </button>` : ''
    }
                    <button class="btn btn-danger btn-small" onclick="removeFile('${file.id}')">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            </div>
            ${file.status === 'uploading' || file.status === 'completed' ? `
                <div class="progress-container">
                    <div class="progress-info">
                        <span>上传进度</span>
                        <span>${file.progress.toFixed(1)}%</span>
                    </div>
                    <div class="progress-bar">
                        <div class="progress-fill" style="width: ${file.progress}%"></div>
                    </div>
                </div>
            ` : ''}
            ${file.error ? `
                <div class="error-message" style="color: var(--danger-color); margin-top: 10px;">
                    <i class="fas fa-exclamation-triangle"></i>
                    ${file.error}
                </div>
            ` : ''}
        </div>
    `).join('');

    fileList.innerHTML = html;
}

// 获取状态图标
function getStatusIcon(status) {
    const icons = {
        waiting: 'fa-clock',
        uploading: 'fa-spinner fa-spin',
        completed: 'fa-check-circle',
        error: 'fa-exclamation-circle',
        paused: 'fa-pause-circle'
    };
    return icons[status] || 'fa-question-circle';
}

// 获取状态文本
function getStatusText(status) {
    const texts = {
        waiting: '等待中',
        uploading: '上传中',
        completed: '已完成',
        error: '上传失败',
        paused: '已暂停'
    };
    return texts[status] || '未知状态';
}

// 过滤文件
function getFilteredFiles() {
    const statusFilter = document.getElementById('statusFilter').value;
    const typeFilter = document.getElementById('typeFilter').value;
    const searchText = document.getElementById('searchInput').value.toLowerCase();

    return fileQueue.filter(file => {
        if (statusFilter && file.status !== statusFilter) return false;
        if (typeFilter && file.type !== typeFilter) return false;
        if (searchText && !file.name.toLowerCase().includes(searchText)) return false;
        return true;
    });
}

// 过滤文件事件
function filterFiles() {
    renderFileList();
}

// 更新统计信息
function updateStats() {
    const totalFiles = fileQueue.length;
    const uploadedFiles = fileQueue.filter(f => f.status === 'completed').length;
    const totalSize = fileQueue.reduce((sum, f) => sum + f.size, 0);

    document.getElementById('totalFiles').textContent = totalFiles;
    document.getElementById('uploadedFiles').textContent = uploadedFiles;
    document.getElementById('totalSize').textContent = formatFileSize(totalSize);
}

// 开始所有上传
async function startAllUploads() {
    const waitingFiles = fileQueue.filter(f => f.status === 'waiting' || f.status === 'paused');

    for (const file of waitingFiles) {
        startUpload(file.id);
        // 添加延迟避免并发过多
        await new Promise(resolve => setTimeout(resolve, 100));
    }
}

// 暂停所有上传
function pauseAllUploads() {
    fileQueue.forEach(file => {
        if (file.status === 'uploading') {
            file.status = 'paused';
        }
    });
    renderFileList();
}

// 清空所有文件
function clearAllFiles() {
    showConfirm('确定要清空所有文件吗？', () => {
        fileQueue = [];
        renderFileList();
        updateStats();
    });
}

// 开始单个文件上传
async function startUpload(fileId) {
    const file = fileQueue.find(f => f.id === fileId);
    if (!file || file.status === 'uploading' || file.status === 'completed') return;

    try {
        file.status = 'uploading';
        file.error = null;
        renderFileList();

        // 计算文件MD5
        if (!file.identifier) {
            showNotification('正在计算文件标识...', 'info');
            file.identifier = await calculateMD5(file.file);
        }

        // 检查是否可以秒传
        const canSecondUpload = await checkSecondUpload(file.identifier, file.name);
        if (canSecondUpload) {
            file.status = 'completed';
            file.progress = 100;
            renderFileList();
            updateStats();
            showNotification(`文件 ${file.name} 秒传成功！`, 'success');
            return;
        }

        // 判断是否需要分片上传
        if (file.size <= 10 * 1024 * 1024) { // 10MB以下直接上传
            await uploadSmallFile(file);
        } else {
            await uploadLargeFile(file);
        }

    } catch (error) {
        file.status = 'error';
        file.error = error.message;
        renderFileList();
        showNotification(`文件 ${file.name} 上传失败: ${error.message}`, 'error');
    }
}

// 暂停单个文件上传
function pauseUpload(fileId) {
    const file = fileQueue.find(f => f.id === fileId);
    if (file && file.status === 'uploading') {
        file.status = 'paused';
        renderFileList();
    }
}

// 移除文件
function removeFile(fileId) {
    showConfirm('确定要移除此文件吗？', () => {
        fileQueue = fileQueue.filter(f => f.id !== fileId);
        renderFileList();
        updateStats();
    });
}

// 检查秒传
async function checkSecondUpload(identifier, fileName) {
    try {
        const response = await fetch(`${API_BASE}/upload/second?identifier=${identifier}&fileName=${encodeURIComponent(fileName)}`, {
            method: 'POST'
        });
        const result = await response.json();
        return result.code === 200 && result.data === true;
    } catch (error) {
        console.warn('秒传检查失败:', error);
        return false;
    }
}

// 小文件上传
async function uploadSmallFile(file) {
    const formData = new FormData();
    formData.append('file', file.file);

    const response = await fetch(`${API_BASE}/upload/tiny?fileName=${encodeURIComponent(file.name)}&identifier=${file.identifier}&size=${file.size}`, {
        method: 'POST',
        body: formData
    });

    const result = await response.json();
    if (result.code !== 200) {
        throw new Error(result.message || '上传失败');
    }

    file.status = 'completed';
    file.progress = 100;
    renderFileList();
    updateStats();
    showNotification(`文件 ${file.name} 上传成功！`, 'success');
}

// 大文件分片上传
async function uploadLargeFile(file) {
    // 初始化分片上传任务
    const initResponse = await fetch(`${API_BASE}/upload/initShardTask`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            fileName: file.name,
            identifier: file.identifier,
            totalSize: file.size,
            chunkSize: CHUNK_SIZE
        })
    });

    const initResult = await initResponse.json();
    if (initResult.code !== 200) {
        throw new Error(initResult.message || '初始化分片上传失败');
    }

    const taskInfo = initResult.data;
    file.uploadId = taskInfo.uploadId;
    file.chunkNum = taskInfo.chunkNum;
    file.uploadedChunks = taskInfo.existPartList || [];

    // 如果已经完成，直接返回
    if (taskInfo.finished) {
        file.status = 'completed';
        file.progress = 100;
        renderFileList();
        updateStats();
        return;
    }

    // 上传分片
    await uploadChunks(file);

    // 合并分片
    const mergeResponse = await fetch(`${API_BASE}/upload/merge/${file.identifier}`, {
        method: 'POST'
    });

    const mergeResult = await mergeResponse.json();
    if (mergeResult.code !== 200) {
        throw new Error(mergeResult.message || '合并分片失败');
    }

    file.status = 'completed';
    file.progress = 100;
    renderFileList();
    updateStats();
    showNotification(`文件 ${file.name} 上传成功！`, 'success');
}

// 上传分片
async function uploadChunks(file) {
    const uploadedPartNumbers = new Set(file.uploadedChunks.map(chunk => chunk.partNumber));

    for (let i = 0; i < file.chunkNum; i++) {
        if (file.status !== 'uploading') break; // 检查是否被暂停

        const partNumber = i + 1;
        if (uploadedPartNumbers.has(partNumber)) {
            // 分片已存在，更新进度
            file.progress = ((i + 1) / file.chunkNum) * 100;
            renderFileList();
            continue;
        }

        const start = i * CHUNK_SIZE;
        const end = Math.min(start + CHUNK_SIZE, file.size);
        const chunk = file.file.slice(start, end);

        const formData = new FormData();
        formData.append('file', chunk);

        const response = await fetch(`${API_BASE}/upload/uploadPart/${file.identifier}/${partNumber}`, {
            method: 'POST',
            body: formData
        });

        const result = await response.json();
        if (result.code !== 200) {
            throw new Error(`分片 ${partNumber} 上传失败: ${result.message}`);
        }

        file.progress = ((i + 1) / file.chunkNum) * 100;
        renderFileList();
    }
}

// 文章处理
async function handleArticleProcess() {
    const title = articleTitle.value.trim();
    const content = articleContent.value.trim();

    if (!content) {
        showNotification('请输入文章内容', 'warning');
        return;
    }

    try {
        showLoading(true);
        processArticleBtn.disabled = true;

        console.log('发送请求到:', `${API_BASE}/upload/article`);
        console.log('请求数据:', { title, content });

        const response = await fetch(`${API_BASE}/upload/article`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                title: title,
                content: content
            })
        });

        console.log('HTTP 响应状态:', response.status);

        // 首先检查 HTTP 状态码
        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`HTTP ${response.status}: ${errorText}`);
        }

        const result = await response.json();
        console.log('后端响应:', result);

        if (result.code !== 200) {
            throw new Error(result.message || '后端处理失败');
        }

        // 更新文章内容
        articleContent.value = result.data;
        showNotification('文章图片处理完成！', 'success');

    } catch (error) {
        console.error('处理错误:', error);
        showNotification(`处理失败: ${error.message}`, 'error');
    } finally {
        showLoading(false);
        processArticleBtn.disabled = false;
    }
}

// 加载服务器文件列表
async function loadServerFiles() {
    try {
        showLoading(true);

        const response = await fetch(`${API_BASE}/upload/list?pageNum=${currentPage}&pageSize=${pageSize}`);
        const result = await response.json();

        if (result.code !== 200) {
            throw new Error(result.message || '加载失败');
        }

        serverFiles = result.data || [];
        renderServerFileList();
        renderPagination();

    } catch (error) {
        showNotification(`加载服务器文件失败: ${error.message}`, 'error');
    } finally {
        showLoading(false);
    }
}

// 渲染服务器文件列表
function renderServerFileList() {
    if (serverFiles.length === 0) {
        serverFileList.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-server"></i>
                <h3>暂无文件</h3>
                <p>服务器上还没有文件</p>
            </div>
        `;
        return;
    }

    const html = serverFiles.map(file => `
        <div class="server-file-item">
            <div class="server-file-info">
                <i class="file-icon fas ${getFileIcon(file.contentType)}"></i>
                <div>
                    <div class="server-file-name">${file.fileName}</div>
                    <div class="server-file-meta">
                        <span><i class="fas fa-hdd"></i> ${formatFileSize(file.fileSize)}</span>
                        <span><i class="fas fa-tag"></i> ${file.contentType}</span>
                        <span><i class="fas fa-clock"></i> ${formatDate(file.uploadTime)}</span>
                    </div>
                </div>
            </div>
            <div class="server-file-actions">
                <button class="btn btn-primary btn-small" onclick="previewFile('${file.url}', '${file.fileName}', '${file.contentType}')">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-success btn-small" onclick="copyUrl('${file.url}')">
                    <i class="fas fa-copy"></i>
                </button>
                <button class="btn btn-danger btn-small" onclick="deleteServerFile('${file.identifier}', '${file.fileName}')">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `).join('');

    serverFileList.innerHTML = html;
}

// 渲染分页
function renderPagination() {
    const totalPages = Math.ceil(serverFiles.length / pageSize);
    const pagination = document.getElementById('pagination');

    let html = '';

    // 上一页
    html += `<button class="page-btn" ${currentPage === 1 ? 'disabled' : ''} onclick="changePage(${currentPage - 1})">
        <i class="fas fa-chevron-left"></i>
    </button>`;

    // 页码
    for (let i = 1; i <= totalPages; i++) {
        if (i === currentPage) {
            html += `<button class="page-btn active">${i}</button>`;
        } else if (i === 1 || i === totalPages || Math.abs(i - currentPage) <= 2) {
            html += `<button class="page-btn" onclick="changePage(${i})">${i}</button>`;
        } else if (i === currentPage - 3 || i === currentPage + 3) {
            html += `<span class="page-btn">...</span>`;
        }
    }

    // 下一页
    html += `<button class="page-btn" ${currentPage === totalPages ? 'disabled' : ''} onclick="changePage(${currentPage + 1})">
        <i class="fas fa-chevron-right"></i>
    </button>`;

    pagination.innerHTML = html;
}

// 切换页面
function changePage(page) {
    currentPage = page;
    loadServerFiles();
}

// 格式化日期
function formatDate(dateString) {
    if (!dateString) return '未知';
    const date = new Date(dateString);
    return date.toLocaleString('zh-CN');
}

// 预览文件
function previewFile(url, fileName, contentType) {
    const modal = document.getElementById('previewModal');
    const content = document.getElementById('previewContent');

    if (contentType.startsWith('image/')) {
        content.innerHTML = `<img src="${url}" style="max-width: 100%; height: auto;" alt="${fileName}">`;
    } else if (contentType === 'application/pdf') {
        content.innerHTML = `<iframe src="${url}" style="width: 100%; height: 500px;" frameborder="0"></iframe>`;
    } else {
        content.innerHTML = `
            <div style="text-align: center; padding: 40px;">
                <i class="fas fa-file" style="font-size: 4rem; color: var(--text-muted); margin-bottom: 20px;"></i>
                <h3>${fileName}</h3>
                <p>无法预览此文件类型</p>
                <a href="${url}" target="_blank" class="btn btn-primary">
                    <i class="fas fa-download"></i> 下载文件
                </a>
            </div>
        `;
    }

    modal.classList.add('show');
}

// 复制URL
function copyUrl(url) {
    navigator.clipboard.writeText(url).then(() => {
        showNotification('链接已复制到剪贴板', 'success');
    }).catch(() => {
        showNotification('复制失败', 'error');
    });
}

// 删除服务器文件
function deleteServerFile(identifier, fileName) {
    showConfirm(`确定要删除文件 "${fileName}" 吗？`, async () => {
        try {
            showLoading(true);

            const response = await fetch(`${API_BASE}/upload/${identifier}`, {
                method: 'DELETE'
            });

            const result = await response.json();
            if (result.code !== 200) {
                throw new Error(result.message || '删除失败');
            }

            showNotification('文件删除成功', 'success');
            loadServerFiles();

        } catch (error) {
            showNotification(`删除失败: ${error.message}`, 'error');
        } finally {
            showLoading(false);
        }
    });
}

// 显示加载状态
function showLoading(show) {
    loadingOverlay.classList.toggle('show', show);
}

// 显示通知
function showNotification(message, type = 'info', duration = 3000) {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.innerHTML = `
        <div class="notification-title">${getNotificationTitle(type)}</div>
        <div class="notification-message">${message}</div>
    `;

    notificationContainer.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideInRight 0.3s ease reverse';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, duration);
}

// 获取通知标题
function getNotificationTitle(type) {
    const titles = {
        success: '成功',
        error: '错误',
        warning: '警告',
        info: '提示'
    };
    return titles[type] || '通知';
}

// 显示确认对话框
function showConfirm(message, callback) {
    const modal = document.getElementById('confirmModal');
    const messageEl = document.getElementById('confirmMessage');
    const okBtn = document.getElementById('confirmOk');

    messageEl.textContent = message;
    modal.classList.add('show');

    okBtn.onclick = function() {
        modal.classList.remove('show');
        callback();
    };
}