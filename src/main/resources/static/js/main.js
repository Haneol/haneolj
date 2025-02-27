// 문서가 로드되면 실행
document.addEventListener('DOMContentLoaded', function() {
    console.log('Document loaded');

    // 프로필 이미지 오류 처리
    const profileImages = document.querySelectorAll('img[onerror]');
    profileImages.forEach(img => {
        img.addEventListener('error', function() {
            console.log('Image failed to load, using default');
        });
    });

    // 폼 유효성 검사 활성화
    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

    // 알림 자동 숨기기
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            const closeButton = alert.querySelector('.btn-close');
            if (closeButton) {
                closeButton.click();
            }
        }, 5000);
    });
});