import { useEffect, useState } from 'react'
import ErrorMessage from '../../components/common/ErrorMessage'
import { addBan, getBans, removeBan } from '../../api/bans'

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

    // 알레르기로 등록한 재료 개수
    const allergyCount = bans.filter(
        (ban) => ban.ban_type === 'ALLERGY'
    ).length

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
            (ban) => ban.ingredient_name === name
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
                <h2 className="section-title">못 먹는 재료</h2>

                <p className="muted BansPage__desc">
                    알레르기 재료는 반드시 제외하고,
                    싫어하는 재료는 가능하면 피하도록 등록합니다.
                </p>

                {allergyCount > 0 && (
                    <p className="muted">
                        알레르기로 등록한 재료: {allergyCount}개
                    </p>
                )}

                {/* 재료 입력 */}
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

                {/* 오류 메시지 */}
                <ErrorMessage message={error} />

                {/* 목록 조회 중 */}
                {loading && (
                    <p className="empty">불러오는 중…</p>
                )}

                {/* 등록된 재료가 없는 경우 */}
                {!loading && !error && bans.length === 0 && (
                    <p className="empty">등록한 재료가 없습니다.</p>
                )}

                {/* 등록된 재료 목록 */}
                {!loading && bans.length > 0 && (
                    <ul className="BansPage__bans">
                        {bans.map((ban) => (
                            <li key={ban.id} className="BansPage__ban">
                                {ban.ban_type === 'ALLERGY' ? (
                                    <span className="tag tag--allergy">
                                        알레르기
                                    </span>
                                ) : (
                                    <span className="tag tag--dislike">
                                        싫어함
                                    </span>
                                )}

                                <span className="BansPage__ban-name">
                                    {ban.ingredient_name}
                                </span>

                                <button
                                    type="button"
                                    className="BansPage__remove"
                                    onClick={() => handleRemoveBan(ban)}
                                    disabled={deletingId !== null}
                                    aria-label={`${ban.ingredient_name} 삭제`}
                                >
                                    {deletingId === ban.id ? '삭제 중…' : '✕'}
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </div>
    )
}