document.addEventListener('DOMContentLoaded', () => {
    // 그래프 초기화 상태를 추적하는 변수
    let graphInitialized = false;

    // 탭 기능 초기화 함수
    const initTabs = () => {
        const tabLinks = document.querySelectorAll('.tab-link');
        const viewContents = document.querySelectorAll('.view-content');

        const setActiveTab = (activeTab) => {
            // 모든 탭과 뷰 콘텐츠 비활성화
            tabLinks.forEach(tab => tab.classList.remove('active'));
            viewContents.forEach(view => view.classList.remove('active'));

            // 클릭한 탭과 해당 뷰 활성화
            activeTab.classList.add('active');
            const viewType = activeTab.getAttribute('data-view');
            const targetView = document.getElementById(`${viewType}-view`);
            if (targetView) {
                targetView.classList.add('active');

                // 그래프 뷰 탭이 선택된 경우 그래프 초기화
                if (viewType === 'graph' && typeof initGraphVisualization === 'function') {
                    // 기존 SVG 요소가 있는지 확인
                    const svg = document.getElementById('graph-svg');

                    // SVG가 없으면 초기화 (중복 초기화 방지)
                    if (!svg) {
                        setTimeout(() => {
                            initGraphVisualization();
                            graphInitialized = true;
                        }, 50);
                    }
                }
            }

            // 탭 변경 시 선택된 상태를 로컬 스토리지에 저장
            localStorage.setItem('selectedView', viewType);
        };

        // 탭 클릭 시 이벤트 핸들러
        tabLinks.forEach(tab => {
            tab.addEventListener('click', () => {
                setActiveTab(tab);
            });
        });

        // 로컬 스토리지에 저장된 마지막 선택 탭 활성화
        const savedView = localStorage.getItem('selectedView');
        if (savedView) {
            const savedTab = document.querySelector(`[data-view="${savedView}"]`);
            if (savedTab) {
                setActiveTab(savedTab);
            }
        } else {
            // 저장된 뷰가 없으면 카테고리 뷰를 기본값으로 설정
            const categoryTab = document.querySelector('[data-view="category"]');
            if (categoryTab) {
                setActiveTab(categoryTab);
            }
        }
    };

    // 디렉토리 트리 기능 초기화 함수
    const initDirectoryTree = () => {
        const directoryItems = document.querySelectorAll('.directory-item');

        directoryItems.forEach(item => {
            item.addEventListener('click', (e) => {
                // 클릭 이벤트가 해당 디렉토리 아이템에서 발생하고, 하위 sub-items를 클릭한 것이 아닐 때만 토글
                if ((e.target === item || e.target.closest('.directory-item') === item) &&
                    !e.target.closest('.sub-items')) {
                    const subItems = item.querySelector('.sub-items');
                    subItems.classList.toggle('collapsed');

                    // 폴더 아이콘 토글 (열림/닫힘)
                    const folderIcon = item.querySelector('.folder-icon i');
                    if (subItems.classList.contains('collapsed')) {
                        folderIcon.classList.remove('fa-folder-open');
                        folderIcon.classList.add('fa-folder');
                    } else {
                        folderIcon.classList.remove('fa-folder');
                        folderIcon.classList.add('fa-folder-open');
                    }
                    e.stopPropagation();
                }
            });
        });
    };

    // 초기화 함수 호출
    initTabs();
    initDirectoryTree();
});