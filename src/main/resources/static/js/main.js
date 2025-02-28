document.addEventListener('DOMContentLoaded', function() {
    const tabLinks = document.querySelectorAll('.tab-link');
    const viewContents = document.querySelectorAll('.view-content');

    // 탭 활성화 함수
    function setActiveTab(activeTab) {
        // 모든 탭 비활성화
        tabLinks.forEach(tab => tab.classList.remove('active'));

        // 모든 뷰 콘텐츠 비활성화
        viewContents.forEach(view => view.classList.remove('active'));

        // 클릭한 탭 활성화
        activeTab.classList.add('active');

        // 해당 뷰 콘텐츠 활성화
        const viewType = activeTab.getAttribute('data-view');
        const targetView = document.getElementById(`${viewType}-view`);
        if (targetView) {
            targetView.classList.add('active');
        }
    }

    // 탭 클릭 이벤트 리스너
    tabLinks.forEach(tab => {
        tab.addEventListener('click', function() {
            setActiveTab(this);
        });
    });

    // 로컬 스토리지에서 마지막 선택 상태 가져오기
    const savedView = localStorage.getItem('selectedView');
    if (savedView) {
        const savedTab = document.querySelector(`[data-view="${savedView}"]`);
        if (savedTab) {
            setActiveTab(savedTab);
        }
    }

    // 탭 변경 시 로컬 스토리지에 저장
    tabLinks.forEach(tab => {
        tab.addEventListener('click', function() {
            const viewType = this.getAttribute('data-view');
            localStorage.setItem('selectedView', viewType);
        });
    });
});