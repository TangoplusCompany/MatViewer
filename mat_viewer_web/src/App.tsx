import { useState, useRef } from 'react'
import dummy from './assets/dummy.png' 
import logo_color from './assets/logo_full.png'
import default_foot from './assets/default_foot.png'
import { domToPng } from 'modern-screenshot'

function App() {
  const certificateRef = useRef<HTMLDivElement>(null)

  const [location] = useState<string>(() => {
    const searchParams = new URLSearchParams(window.location.search)
    const locationParam = searchParams.get('location')
    return locationParam ? decodeURIComponent(locationParam) : '위치 정보 없음'
  })
  const [ratio] = useState(() => {
    const searchParams = new URLSearchParams(window.location.search)
    const ratioParam = searchParams.get('ratio') // "45_55-40_60"
    
    if (ratioParam && ratioParam.includes('-')) {
      const parts = ratioParam.split('-')
      const first = parts[0]  // "45_55"
      const second = parts[1] // "40_60"
      const [left, right] = first.split("_")
      const [top, bottom] = second.split("_")
      return { left, right, top, bottom }
    }
    return null
  })
  console.log(ratio)
  const [imageUrl] = useState<string>(() => {
    const searchParams = new URLSearchParams(window.location.search)
    const imageName = searchParams.get('image') // 예: heatmap_20260626_104910@a24e9a9d097dd0b8.png
    console.log(imageName)
    if (imageName) {
      // 🔴 파일명에 @ 등이 들어가므로 encodeURIComponent로 감싸줍니다.
      return `https://ftxrnhukotlmfobdabwo.supabase.co/storage/v1/object/public/temp_mat_images/${encodeURIComponent(imageName)}`
    }
    return dummy 
  })
  const [measurementDate] = useState<string>(() => {
    const searchParams = new URLSearchParams(window.location.search)
    const imageName = searchParams.get('image') 

    if (imageName && imageName.includes('_')) {
      try {
        const parts = imageName.split('_') // ["heatmap", "20260626", "104910@a24e9a9d097dd0b8.png"]
        const datePart = parts[1] // "20260626"
        
        // 🔴 뒤에 기기ID가 붙어있으므로 @ 기점으로 한 번 더 쪼갭니다.
        const timeWithId = parts[2] // "104910@a24e9a9d097dd0b8.png"
        const timePart = timeWithId.split('@')[0] // "104910"

        const year = datePart.substring(0, 4)
        const month = datePart.substring(4, 6)
        const day = datePart.substring(6, 8)

        const hour = timePart.substring(0, 2)
        const minute = timePart.substring(2, 4)
        const second = timePart.substring(4, 6)

        return `${year}년 ${month}월 ${day}일\n${hour}:${minute}:${second}`
      } catch (e) {
        console.error("날짜 파싱 실패", e)
      }
    }
    return "XXXX년 XX월 XX일\nXX:XX:XX"
  })
  const [loading] = useState<boolean>(false) 
  const [error] = useState<string | undefined>(undefined)

  const handleDownload = async () => {
    if (!certificateRef.current) return
    try {
      // html2canvas 대신 domToPng 사용
      const dataUrl = await domToPng(certificateRef.current, {
        scale: 2,
        backgroundColor: '#2F3034'
      })

      const link = document.createElement('a')
      link.href = dataUrl
      link.download = `mat_result_${Date.now()}.png`
      link.click()
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
              
              {/* 하단 텍스트 영역 */}
              <div className="grid grid-cols-3 h-24 w-full px-2 mb-2 text-center text-sm tracking-wide">
                <div className="h-full text-sub100 bg-sub800 mr-2 rounded-xs p-2 flex flex-col items-center justify-center">
                  <span className='font-semibold text-base mb-1 whitespace-nowrap'>측정 장소</span>
                  {location && `${location}`}
                </div>
                
                {/* 🔴 [수정] 측정 일자: \n을 기준으로 명확히 쪼개서 <br /> 처리하여 강제 줄바꿈 보장 */}
                <div className="h-full text-sub100 bg-sub800 mr-2 rounded-xs p-2 flex flex-col items-center justify-center">
                  <span className='font-semibold text-base mb-1 whitespace-nowrap'>측정 일자</span>
                  <div className="whitespace-nowrap">
                    {measurementDate.split('\n').map((line, index) => (
                      <span key={index}>
                        {line}
                        {index === 0 && <br />}
                      </span>
                    ))}
                  </div>
                </div>
                
                {/* 🔴 [수정] 현재 족압: 숫자가 박스를 밀어내지 않도록 각각 whitespace-nowrap 강제 적용 */}
                <div className="h-full text-sub100 bg-sub800 mr-2 rounded-xs p-2 flex flex-col items-center justify-center">
                  <span className='font-semibold text-base mb-1 whitespace-nowrap'>현재 족압</span>
                  <span className="whitespace-nowrap">
                    좌우: {ratio?.left}% / {ratio?.right}%
                  </span>
                  <span className="whitespace-nowrap">
                    상하: {ratio?.top}% / {ratio?.bottom}%
                  </span>
                </div>
              </div>
            </div>

            <div className='flex flex-col w-full h-full bg-sub900 rounded-xl p-2 '>
              <span className='font-semibold text-white text-start text-base pl-1'>결과 설명</span>
              
              <div className='flex gap-2 items-center bg-sub900 p-2 rounded-2xl w-full '>
                {/* 왼쪽: 족압 이미지 영역 */}
                <div className='flex gap-1 flex-col w-fit items-center flex-shrink-0'>
                  <div className='text-sub100 bg-sub800 text-xs px-2 py-1 rounded-md w-full text-center font-medium whitespace-nowrap'>
                    표준 족압 형태
                  </div>
                  <img src={default_foot} className='w-32 h-32 rounded-xl object-contain' alt="기본 족압" />
                </div>

                {/* 오른쪽: 직관적인 족압 가이드 설명 영역 */}
                <div className='flex flex-col gap-3 text-start flex-1 min-w-0'>
                  
                  {/* 1. 좌우 균형 지표 */}
                  <div className='flex flex-col gap-0.5'>
                    <div className='flex items-center gap-1.5'>
                      <span className='text-xs text-white font-bold whitespace-nowrap'>좌우 균형</span>
                      <span className='text-xs text-sub300 font-semibold bg-sub800 px-1.5 py-0.5 rounded whitespace-nowrap'>표준 50% : 50%</span>
                    </div>
                    <p className='text-xs text-sub200 leading-relaxed pl-0.5'>
                      오차 <span className='text-emerald-400 font-semibold whitespace-nowrap'>±5% 이내</span>가 정상이며, 10% 이상 차이가 날 경우 골반과 척추 불균형을 유발할 수 있습니다.
                    </p>
                  </div>

                  {/* 2. 앞뒤 균형 지표 */}
                  <div className='flex flex-col gap-0.5'>
                    <div className='flex items-center gap-1.5'>
                      <span className='text-xs text-white font-bold whitespace-nowrap'>앞뒤 균형</span>
                      <span className='text-xs text-sub300 font-semibold bg-sub800 px-1.5 py-0.5 rounded whitespace-nowrap'>표준 40% : 60%</span>
                    </div>
                    <p className='text-xs text-sub200 leading-relaxed pl-0.5'>
                      뒤꿈치가 체중을 지지하는 정석 비율입니다. 오차 <span className='text-emerald-400 font-semibold whitespace-nowrap'>10% 초과 시</span> 앞/뒤 상체 쏠림이 심한 상태입니다.
                    </p>
                  </div>

                  {/* 하단 팁 */}
                  <div className='text-xs text-sub400 pt-1.5 mt-0.5 leading-tight'>
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