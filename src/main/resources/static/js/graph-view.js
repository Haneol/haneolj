document.addEventListener('DOMContentLoaded', function() {
  // 그래프 시각화 즉시 초기화
  initGraphVisualization();
});

function initGraphVisualization() {
  const graphContainer = document.getElementById('graph-container');
  if (!graphContainer) return;

  // 로딩 중 플레이스 홀더
  const placeholder = document.querySelector('.graph-placeholder');
  if (placeholder) {
    placeholder.innerHTML = `
      <div class="loading-spinner"></div>
      <h3>그래프 데이터 로딩 중...</h3>
    `;
    placeholder.style.display = 'block';
  }

  // 그래프 크기 설정
  const width = graphContainer.clientWidth;
  const height = graphContainer.clientHeight;

  // 그래프 컨테이너 높이 조정
  graphContainer.style.height = height + 'px';

  // SVG 생성
  const svg = d3.select('#graph-container')
  .append('svg')
  .attr('id', 'graph-svg')
  .attr('width', width)
  .attr('height', height);

  // 메인 그룹 생성
  const g = svg.append('g');

  // API에서 그래프 데이터 가져오기
  fetchGraphData()
  .then(graphData => {
    // 로딩 끝, 플레이스홀더 숨기기
    if (placeholder) placeholder.style.display = 'none';

    // 데이터가 있으면 그래프 렌더링
    if (graphData && graphData.nodes && graphData.nodes.length > 0) {
      renderGraph(graphData, g, svg, width, height);
    } else {
      // 데이터가 없으면 메시지 표시
      if (placeholder) {
        placeholder.innerHTML = `
            <i class="fas fa-info-circle"></i>
            <h3>그래프 데이터가 없습니다</h3>
          `;
        placeholder.style.display = 'block';
      }
    }
  })
  .catch(error => {
    console.error('그래프 데이터 로드 오류:', error);

    // 오류 발생 시 메시지 표시
    if (placeholder) {
      placeholder.innerHTML = `
          <i class="fas fa-exclamation-triangle"></i>
          <h3>그래프 데이터 로드 중 문제가 발생했습니다.</h3>
        `;
      placeholder.style.display = 'block';
    }
  });
}

// 서버 API에서 그래프 데이터 가져오기
async function fetchGraphData() {
  try {
    const response = await fetch('/api/study/graph');

    if (!response.ok) {
      if (response.status === 204) {
        // 204 No Content는 데이터가 없는 경우
        console.log('그래프 데이터가 없습니다.');
        return { nodes: [], links: [] };
      }
      throw new Error(`API 오류: ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    console.error('그래프 데이터 fetch 오류:', error);
    throw error;
  }
}

// 그래프 렌더링 함수
function renderGraph(graphData, g, svg, width, height) {
  const nodes = graphData.nodes;
  const links = graphData.links;

  // 중심점 계산
  const centerX = width / 2;
  const centerY = height / 2;

  // 노드 초기 위치 설정
  nodes.forEach((node, i) => {
    // 노드를 원형으로 고르게 배치
    const angle = (2 * Math.PI * i) / nodes.length;

    // 중심에 가까운 위치에서 시작
    const initialRadius = Math.min(width, height) * 0.2 * Math.random();

    node.x = centerX + initialRadius * Math.cos(angle);
    node.y = centerY + initialRadius * Math.sin(angle);
  });

  // 툴팁 생성
  const tooltip = d3.select('body').append('div')
  .attr('class', 'graph-tooltip')
  .style('position', 'absolute')
  .style('background-color', '#F8FAFC')
  .style('padding', '8px')
  .style('border-radius', '10px')
  .style('box-shadow', '0 2px 4px rgba(0,0,0,0.2)')
  .style('pointer-events', 'none')
  .style('opacity', 0)
  .style('z-index', 1000);

  // 시뮬레이션 설정
  const simulation = d3.forceSimulation(nodes)
  // 링크 거리 설정
  .force('link', d3.forceLink(links)
  .id(d => d.id)
  .distance(60))
  // 반발력 설정
  .force('charge', d3.forceManyBody()
  .strength(-80))
  // 중력 설정
  .force('center', d3.forceCenter(centerX, centerY)
  .strength(0.1))
  // 노드들이 중심에서 다양한 거리에 위치하도록 설정
  .force('radial', d3.forceRadial(
      function(d, i) {
        const radiusFactor = 0.1 + (Math.random() * 0.25);
        return Math.min(width, height) * 0.35 * radiusFactor;
      },
      centerX,
      centerY
  ).strength(0.2))
  // 노드 간 충돌 방지
  .force('collide', d3.forceCollide()
  .radius(15)
  .strength(0.8));

  // 시뮬레이션 설정
  simulation
  .alpha(0.9)
  .alphaDecay(0.015)
  .velocityDecay(0.3);

  // 초기 시뮬레이션 시작
  simulation.restart();

  // 링크 생성
  const link = g.append('g')
  .attr('class', 'links')
  .selectAll('line')
  .data(links)
  .enter()
  .append('line')
  .attr('stroke', '#CBD5E1')
  .attr('stroke-width', 1)
  .attr('opacity', 0.5);

  // 노드 생성
  const node = g.append('g')
  .attr('class', 'nodes')
  .selectAll('circle')
  .data(nodes)
  .enter()
  .append('circle')
  .attr('r', 6)
  .attr('fill', '#94A3B8') // 회색
  .style('cursor', 'pointer')
  .call(drag(simulation));

  // 노드 라벨 생성
  const labels = g.append('g')
  .attr('class', 'node-labels')
  .selectAll('text')
  .data(nodes)
  .enter()
  .append('text')
  .attr('x', d => d.x)
  .attr('y', d => d.y + 16)
  .attr('text-anchor', 'middle')
  .attr('font-size', '6px')
  .attr('fill', '#64748B')
  .attr('opacity', 0)
  .text(d => d.name);

  // 이벤트 동작

  // 노드 호버 이벤트
  node.on('mouseover', function(event, d) {
    // 개별 노드
    d3.select(this)
    .interrupt('hover')
    .transition('hover')
    .duration(100)
    .attr('r', 9)
    .attr('fill', '#EE5F6A');

    // 연결된 링크 강조
    link.transition('linkHover')
    .duration(100)
    .attr('stroke', function(l) {
      return (l.source.id === d.id || l.target.id === d.id) ? '#EE5F6A' : '#CBD5E1';
    })
    .attr('stroke-width', function(l) {
      return (l.source.id === d.id || l.target.id === d.id) ? 2 : 1;
    })
    .attr('opacity', function(l) {
      return (l.source.id === d.id || l.target.id === d.id) ? 0.8 : 0.3;
    });

    // 연결되지 않은 노드들의 투명도 조절
    node.filter(n => n.id !== d.id)
    .interrupt('fade')
    .transition('fade')
    .duration(100)
    .attr('opacity', function(n) {
      // 연결된 노드는 그대로 유지
      const isConnected = links.some(function(l) {
        return (l.source.id === d.id && l.target.id === n.id) ||
            (l.source.id === n.id && l.target.id === d.id);
      });
      return isConnected ? 1 : 0.3;
    });

    // 해당 노드의 라벨 강조
    labels.filter(n => n.id === d.id)
    .interrupt('labelHover')
    .transition('labelHover')
    .duration(100)
    .attr('opacity', 1)
    .attr('fill', '#EE5F6A');

    // 툴팁 표시
    tooltip.transition()
    .duration(200)
    .style('opacity', 1);
    tooltip.html(d.name)
    .style('border-radius', '10px')
    .style('color', '#0F172A')
    .style('left', (event.pageX + 10) + 'px')
    .style('top', (event.pageY - 28) + 'px');
  })
  // 노드 마우스 아웃 이벤트
  .on('mouseout', function(event, d) {
    // 개별 노드 복원
    d3.select(this)
    .interrupt('hover')
    .transition('hover')
    .duration(300)
    .attr('r', 6)
    .attr('fill', '#94A3B8');

    // 링크 복원
    link.transition('linkReset')
    .duration(200)
    .attr('stroke', '#CBD5E1')
    .attr('stroke-width', 1)
    .attr('opacity', 0.5);

    // 모든 노드의 투명도 복원
    node.transition('fade')
    .duration(200)
    .attr('opacity', 1);

    // 라벨 복원
    const currentZoom = d3.zoomTransform(svg.node()).k;
    labels.filter(n => n.id === d.id)
    .interrupt('labelHover')
    .transition('labelHover')
    .duration(200)
    .attr('opacity', currentZoom >= 2 ? 0.8 : 0)
    .attr('fill', '#64748B');

    // 툴팁 숨기기
    tooltip.transition()
    .duration(500)
    .style('opacity', 0);
  })
  // 노드 클릭 시 해당 문서로 이동
  .on('click', function(event, d) {
    if (d.encodedPath) {
      window.location.href = `/study/view/${d.encodedPath}`;
    }
  });

  // 시뮬레이션 틱 이벤트
  simulation.on('tick', function() {
    // 노드 위치 업데이트
    node
    .attr('cx', function(d) { return d.x; })
    .attr('cy', function(d) { return d.y; });

    // 링크 위치 업데이트
    link
    .attr('x1', function(d) { return d.source.x; })
    .attr('y1', function(d) { return d.source.y; })
    .attr('x2', function(d) { return d.target.x; })
    .attr('y2', function(d) { return d.target.y; });

    // 라벨 위치 업데이트
    labels
    .attr('x', function(d) { return d.x; })
    .attr('y', function(d) { return d.y + 16; });
  });


  // 줌 기능
  const zoom = d3.zoom()
  .scaleExtent([0.4, 4])
  .on('zoom', function(event) {
    g.attr('transform', event.transform);

    const zoomLevel = event.transform.k;

    // 줌이 2 이상이면 라벨 표시, 아니면 숨김
    labels.transition()
    .duration(300)
    .attr('opacity', function() {
      // 줌에 따라 점진적으로 투명도 변경
      if (zoomLevel < 1.5) return 0;
      if (zoomLevel > 2) return 1;

      return ((zoomLevel - 1.5)) * 2;
    });
  });

  svg.call(zoom);

  // 드래그 기능
  function drag(simulation) {
    function dragstarted(event) {
      if (!event.active) simulation.alphaTarget(0.3).restart();
      event.subject.fx = event.subject.x;
      event.subject.fy = event.subject.y;
    }

    function dragged(event) {
      event.subject.fx = event.x;
      event.subject.fy = event.y;
    }

    function dragended(event) {
      if (!event.active) simulation.alphaTarget(0);
      event.subject.fx = null;
      event.subject.fy = null;
    }

    return d3.drag()
    .on('start', dragstarted)
    .on('drag', dragged)
    .on('end', dragended);
  }
}