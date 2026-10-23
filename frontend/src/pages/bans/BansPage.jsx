import { useEffect, useState } from 'react'
import Icon from '../../components/common/Icon'
import ErrorMessage from '../../components/common/ErrorMessage'
import { addBan, getBans, removeBan } from '../../api/bans'

// 재료 앞에 붙일 그림문자.
// 이름에 아래 낱말이 들어 있으면 그 문자를 쓴다. 위에서부터 찾으므로
// 좁은 말(고등어)을 넓은 말(생선)보다 먼저 적어야 한다.
const BAN_EMOJIS = [
    { emoji: '🐟', words: ['고등어', '갈치', '굴비', '동태', '명태', '황태', '북어', '조기', '삼치', '꽁치', '가자미', '복어', '연어', '참치', '멸치', '생선'] },
    { emoji: '🦐', words: ['새우'] },
    { emoji: '🦑', words: ['오징어', '낙지', '문어', '주꾸미'] },
    { emoji: '🦀', words: ['꽃게', '게살'] },
    { emoji: '🦪', words: ['굴', '조개', '홍합', '전복', '바지락', '해산물'] },
    { emoji: '🥚', words: ['계란', '달걀'] },
    { emoji: '🥛', words: ['우유', '유제품'] },
    { emoji: '🧀', words: ['치즈'] },
    { emoji: '🥜', words: ['땅콩', '견과', '호두', '아몬드', '잣'] },
    { emoji: '🫘', words: ['콩', '두부'] },
    { emoji: '🌾', words: ['밀가루', '밀', '보리'] },
    { emoji: '🥒', words: ['오이'] },
    { emoji: '🥬', words: ['배추', '상추', '시금치', '깻잎', '야채', '채소', '나물'] },
    { emoji: '🧅', words: ['양파'] },
    { emoji: '🧄', words: ['마늘'] },
    { emoji: '🌿', words: ['부추', '미나리', '파'] },
    { emoji: '🥔', words: ['감자'] },
    { emoji: '🍠', words: ['고구마'] },
    { emoji: '🥕', words: ['당근'] },
    { emoji: '🍅', words: ['토마토'] },
    { emoji: '🌶️', words: ['고추', '매운'] },
    { emoji: '🍄', words: ['버섯'] },
    { emoji: '🍜', words: ['칼국수', '국수', '라면', '면'] },
    { emoji: '🍚', words: ['쌀', '밥'] },
    { emoji: '🥩', words: ['소고기', '돼지고기', '삼겹', '고기'] },
    { emoji: '🍗', words: ['닭'] },
    { emoji: '🍓', words: ['딸기'] },
    { emoji: '🍑', words: ['복숭아'] },
    { emoji: '🥝', words: ['키위'] },
]

// 재료 이름에 맞는 그림문자를 찾는다. 없으면 기본 문자를 돌려준다
function getBanEmoji(name) {
    for (let i = 0; i < BAN_EMOJIS.length; i++) {
        const item = BAN_EMOJIS[i]

        for (let j = 0; j < item.words.length; j++) {
            if (name.includes(item.words[j])) {
                return item.emoji
            }
        }
    }

    return '🍽️'
}

// 서버 오류를 화면에 표시할 문장으로 변환
function getErrorMessage(err) {
    if (err.response) {
        const detail = err.response.data?.detail

        if (Array.isArray(detail)) {
            return detail.map((item) => item.msg).filter(Boolean).join(', ')
                || '요청을 처리하지 못했습니다.'
        }

        return typeof detail === 'string'
            ? detail
            : '요청을 처리하지 못했습니다.'
    }

    if (err.request) {
        return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.'
    }

    return err.message || '요청을 처리하지 못했습니다.'
}

export default function BansPage() {
    // 목록과 입력값
    const [bans, setBans] = useState([])
    const [banName, setBanName] = useState('')
    const [banType, setBanType] = useState('DISLIKE')

    // 로딩, 저장, 오류 상태
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [deletingId, setDeletingId] = useState(null)
    const [error, setError] = useState('')

    // 유형별로 나눠 센다
    const allergyBans = bans.filter((ban) => ban.banType === 'ALLERGY')
    const dislikeBans = bans.filter((ban) => ban.banType === 'DISLIKE')

    // 카드 위 요약 숫자 세 칸
    const banStats = [
        { key: 'all', label: '전체 재료', value: bans.length },
        { key: 'allergy', label: '알레르기', value: allergyBans.length },
        { key: 'dislike', label: '싫어하는 재료', value: dislikeBans.length },
    ]

    // 꼭 피해야 하는 알레르기를 앞에 놓는다
    const sortedBans = allergyBans.concat(dislikeBans)

    // 페이지가 처음 열릴 때 재료 목록 조회
    useEffect(() => {
        let cancelled = false

        const loadBans = async () => {
            try {
                const data = await getBans()

                if (!cancelled) {
                    setBans(data)
                }
            } catch (err) {
                if (!cancelled) {
                    setError(getErrorMessage(err))
                }
            } finally {
                if (!cancelled) {
                    setLoading(false)
                }
            }
        }

        loadBans()

        // 다른 페이지로 이동한 뒤에는 상태를 변경하지 않음
        return () => {
            cancelled = true
        }
    }, [])

    // 재료 추가
    const handleAddBan = async (e) => {
        e.preventDefault()

        if (saving) return

        const name = banName.trim()

        if (!name) {
            setError('재료 이름을 입력해주세요.')
            return
        }

        const already = bans.some(
            (ban) => ban.ingredientName === name
        )

        if (already) {
            setError('이미 등록한 재료입니다.')
            return
        }

        setError('')
        setSaving(true)

        try {
            const created = await addBan({
                ingredientName: name,
                banType,
            })

            setBans((prev) => [...prev, created])
            setBanName('')
        } catch (err) {
            setError(getErrorMessage(err))
        } finally {
            setSaving(false)
        }
    }

    // 재료 삭제
    const handleRemoveBan = async (ban) => {
        if (deletingId !== null) return

        setError('')
        setDeletingId(ban.id)

        try {
            await removeBan(ban.id)

            setBans((prev) =>
                prev.filter((item) => item.id !== ban.id)
            )
        } catch (err) {
            setError(getErrorMessage(err))
        } finally {
            setDeletingId(null)
        }
    }

    return (
        <div className="BansPage">
            <section className="card">
                {/* 제목 + 등록 개수 */}
                <div className="BansPage__header">
                    <span className="BansPage__header-icon">
                        <Icon name="ban" size={20} />
                    </span>

                    <div className="BansPage__header-text">
                        <h2 className="section-title">못 먹는 재료 관리</h2>
                        <p className="muted BansPage__desc">
                            나에게 맞는 메뉴 추천을 위한 식재료 설정
                        </p>
                    </div>

                    {!loading && (
                        <span className="BansPage__count">
                            {bans.length}개 등록
                        </span>
                    )}
                </div>

                {/* 요약 숫자 세 칸 */}
                <div className="BansPage__stats">
                    {banStats.map((stat) => (
                        <div key={stat.key} className="BansPage__stat">
                            <strong className="BansPage__stat-value">
                                {loading ? '-' : stat.value}
                            </strong>
                            <span className="BansPage__stat-label">
                                {stat.label}
                            </span>
                        </div>
                    ))}
                </div>

                {/* 재료 입력 */}
                <div className="BansPage__add">
                    <p className="BansPage__label">재료 추가하기</p>

                    <form className="BansPage__form" onSubmit={handleAddBan}>
                        <input
                            type="text"
                            value={banName}
                            onChange={(e) => setBanName(e.target.value)}
                            placeholder="재료 이름 (예: 오이)"
                            aria-label="재료 이름"
                            maxLength={100}
                            disabled={loading || saving}
                        />

                        <select
                            value={banType}
                            onChange={(e) => setBanType(e.target.value)}
                            aria-label="제외 유형"
                            disabled={loading || saving}
                        >
                            <option value="DISLIKE">싫어함</option>
                            <option value="ALLERGY">알레르기</option>
                        </select>

                        <button
                            type="submit"
                            className="btn"
                            disabled={loading || saving}
                        >
                            {saving ? '추가 중…' : '추가'}
                        </button>
                    </form>
                </div>

                {/* 오류 메시지 */}
                <ErrorMessage message={error} />

                {/* 등록된 재료 */}
                <div className="BansPage__list">
                    <div className="BansPage__list-head">
                        <h3 className="BansPage__list-title">등록된 재료</h3>

                        {!loading && bans.length > 0 && (
                            <span className="BansPage__list-count">
                                {bans.length}개
                            </span>
                        )}
                    </div>

                    {/* 목록 조회 중 */}
                    {loading && (
                        <p className="empty">불러오는 중…</p>
                    )}

                    {/* 등록된 재료가 없는 경우 */}
                    {!loading && !error && bans.length === 0 && (
                        <p className="empty">등록한 재료가 없습니다.</p>
                    )}

                    {/* 재료 칩. 알레르기는 테두리 색으로 구분한다 */}
                    {!loading && bans.length > 0 && (
                        <ul className="BansPage__bans">
                            {sortedBans.map((ban) => (
                                <li
                                    key={ban.id}
                                    className={
                                        ban.banType === 'ALLERGY'
                                            ? 'BansPage__ban BansPage__ban--allergy'
                                            : 'BansPage__ban'
                                    }
                                    title={
                                        ban.banType === 'ALLERGY'
                                            ? '알레르기'
                                            : '싫어함'
                                    }
                                >
                                    <span
                                        className="BansPage__ban-emoji"
                                        aria-hidden="true"
                                    >
                                        {getBanEmoji(ban.ingredientName)}
                                    </span>

                                    <span className="BansPage__ban-name">
                                        {ban.ingredientName}
                                    </span>

                                    <button
                                        type="button"
                                        className="BansPage__remove"
                                        onClick={() => handleRemoveBan(ban)}
                                        disabled={deletingId !== null}
                                        aria-label={`${ban.ingredientName} 삭제`}
                                    >
                                        {deletingId === ban.id ? '…' : '✕'}
                                    </button>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </section>
        </div>
    )
}
