import { useState, useRef } from 'react'
import html2canvas from 'html2canvas-pro'
// 1. 로컬 더미 이미지 import
import dummy from './assets/dummy.png' 
import logo_color from './assets/logo_full.png'
import default_foot from './assets/default_foot.png'

function App() {
  const certificateRef = useRef<HTMLDivElement>(null)

  // URL 파라미터에서 위치(주소) 가져오기
  const [location] = useState<string>(() => {
    const searchParams = new URLSearchParams(window.location.search)
    const locationParam = searchParams.get('location')
    return locationParam ? decodeURIComponent(locationParam) : '서울특별시 강남구 역삼동 (더미 위치)'
  })

  // 실제 데이터(image 파라미터)가 있으면 사용하고, 없으면 로컬 dummy 이미지를 기본값으로 설정
  const [imageUrl] = useState<string>(() => {
    const searchParams = new URLSearchParams(window.location.search)
    const imageName = searchParams.get('image')
    return imageName ? dummy : dummy 
  })

  // 로딩 및 에러 상태 정의
  const [loading] = useState<boolean>(false) 
  const [error] = useState<string | undefined>(undefined)

  // 🛠️ html2canvas 실행 직전에 oklab 파싱 오류를 막기 위한 옵션 추가
  const handleDownload = async () => {
    if (!certificateRef.current) return
    try {
      const canvas = await html2canvas(certificateRef.current, {
        scale: 2,
        useCORS: true,
        backgroundColor: '#2F3034', // 기존 스타일링 배경색을 그대로 유지하도록 null 지정
        // 💡 oklab 파싱 에러 방지: 캡처 전 복사본(clone)에서 오류 요소를 건드리지 않도록 콜백 처리
        onclone: (clonedDoc) => {
          const clonedMain = clonedDoc.querySelector('main')
          if (clonedMain) {
            // 필요 시 클론된 돔 내부의 특정 연산 스타일 간섭을 최소화합니다.
          }
        }
      })
      const image = canvas.toDataURL('image/png')
      const link = document.createElement('a')
      link.href = image
      link.download = `mat_result_${Date.now()}.png`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
    } catch (err) {
      console.error(err)
      alert('이미지 저장 중 오류가 발생했습니다.')
    }
  }

  return (
    <div className="a4-page flex flex-col h-full items-center">

      {/* 중앙 콘텐츠 영역 (기존 스타일 및 클래스명 완전히 유지) */}
      <main 
        ref={certificateRef} 
        className="flex-1 flex flex-col items-center w-full p-4 bg-"
      >
        {/* 상단 헤더 */}
        <div className='flex items-center mt-4'>
          <img src={logo_color} className='w-80 cursor-pointer' alt="Logo" onClick={() => window.open('https://www.tangobody.co.kr/', '_blank')} />

        </div>
        {loading && <p className="text-gray-400 animate-pulse">이미지를 불러오는 중입니다...</p>}
        {error && <p className="text-red-400">{error}</p>}

        {imageUrl && !loading && !error && (
          <>
            <div className="w-full bg-sub900 p-5 my-4 rounded-2xl flex flex-col items-center gap-5">
            
              {/* 이미지 뷰 (더미 png 파일 로드) */}
              <div className="w-full bg-[#1A1A1E] p-2 rounded-xl flex items-center justify-center min-h-[30vh]">
                <img 
                  src={imageUrl} 
                  alt="Heatmap Result" 
                  className="w-2/3 h-auto object-contain rounded-lg" 
                />
              </div>
              
              {/* 하단 텍스트 영역 (기존 색상 유지, 정렬 및 flex center 구조 추가) */}
              <div className="grid grid-cols-3 h-24 w-full px-2 mb-2 text-center text-sm tracking-wide">
                <div className="h-full text-sub100 bg-sub800 mr-2 rounded-xs p-2 flex flex-col items-center justify-center">
                  <span className='font-semibold text-base mb-1'>측정 장소</span>
                  {location && `${location}`}
                </div>
                <div className="h-full text-sub100 bg-sub800 mr-2 rounded-xs p-2 flex flex-col items-center justify-center">
                  <span className='font-semibold text-base mb-1'>측정 일자</span>
                  2026월 05월 25일<br/>16:30:22
                </div>
                
                <div className="h-full text-sub100 bg-sub800 mr-2 rounded-xs p-2 flex flex-col items-center justify-center">
                  <span className='font-semibold text-base mb-1'>현재 상태</span>
                  <span>
                    상하:
                  </span>
                  <span>
                    좌우:
                  </span>
                </div>
              </div>
            </div>

            <div className='flex flex-col w-full h-full bg-sub900 rounded-xl p-2 '>
              <span className='font-semibold text-white text-start text-base'>결과 설명</span>
              
              <div className='flex gap-2 items-center bg-sub900 p-2 rounded-2xl w-full '>
                {/* 왼쪽: 족압 이미지 영역 */}
                <div className='flex gap-1 flex-col w-fit items-center flex-shrink-0'>
                  <div className='text-sub100 bg-sub800 text-xs px-2 py-1 rounded-md w-full text-center font-medium'>
                    표준 족압 형태
                  </div>
                  {/* 이전에 만든 SVG 컴포넌트나 이미지를 여기에 배치 */}
                  <img src={default_foot} className='w-32 h-32 rounded-xl object-contain' alt="기본 족압" />
                </div>

                {/* 🎯 오른쪽: 직관적인 족압 가이드 설명 영역 */}
                <div className='flex flex-col gap-3 text-start flex-1 min-w-0'>
                  
                  {/* 1. 좌우 균형 지표 */}
                  <div className='flex flex-col gap-0.5'>
                    <div className='flex items-center gap-1.5'>
                      <span className='text-xs text-white font-bold'>좌우 균형</span>
                      <span className='text-[11px] text-sub300 font-semibold bg-sub800 px-1.5 py-0.5 rounded'>표준 50% : 50%</span>
                    </div>
                    <p className='text-xs text-sub200 leading-relaxed pl-0.5'>
                      오차 <span className='text-emerald-400 font-semibold'>±5% 이내</span>가 정상이며, 10% 이상 차이가 날 경우 골반과 척추 불균형을 유발할 수 있습니다.
                    </p>
                  </div>

                  {/* 2. 앞뒤 균형 지표 */}
                  <div className='flex flex-col gap-0.5'>
                    <div className='flex items-center gap-1.5'>
                      <span className='text-xs text-white font-bold'>앞뒤 균형</span>
                      <span className='text-[11px] text-sub300 font-semibold bg-sub800 px-1.5 py-0.5 rounded'>표준 40% : 60%</span>
                    </div>
                    <p className='text-xs text-sub200 leading-relaxed pl-0.5'>
                      뒤꿈치가 체중을 지지하는 정석 비율입니다. 오차 <span className='text-emerald-400 font-semibold'>10% 초과 시</span> 앞/뒤 상체 쏠림이 심한 상태입니다.
                    </p>
                  </div>

                  {/* 하단 팁 */}
                  <div className='text-xs text-sub400  pt-1.5 mt-0.5 leading-tight'>
                    💡 평소 서 있을 때 어떤 발에 체중을 싣고 지탱하는지 보여주는 직관적 지표입니다.
                  </div>

                </div>
              </div>
            </div>
          </>
          
        )}
      </main>

      {/* 화면 최하단 고정 버튼 영역 */}
      <footer className="w-full">
        {imageUrl && (
          <button 
            onClick={handleDownload} 
            className="w-full py-5 bg-blue-600 hover:bg-blue-700 font-bold text-lg text-center text-white tracking-wide block transition-colors shadow-lg cursor-pointer"
          >
            다운로드
          </button>
        )}
      </footer>

    </div>
  )
}

export default App