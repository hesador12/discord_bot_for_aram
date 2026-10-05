package com.aram.bot.domain;

import com.aram.bot.repository.MemberRepository;

import java.sql.SQLException;

public class SettlementService {

    private final MemberRepository repository = new MemberRepository();

    public void registerMember(String name) throws SQLException {
        if (repository.existsByName(name)) {
            throw new IllegalArgumentException("`" + name + "`님은 이미 등록되어 있습니다.");
        }
        repository.save(name);
    }

    public void deleteMember(String name) throws SQLException {
        if (!repository.existsByName(name)) {
            throw new IllegalArgumentException("`" + name + "`님은 존재하지 않는 멤버입니다.");
        }
        repository.delete(name);
    }

    public void settle(String winner, String loser, int amount) throws SQLException {
        if (!repository.existsByName(winner)) {
            throw new IllegalArgumentException("`" + winner + "`님은 등록되지 않은 인원입니다.");
        }
        if (!repository.existsByName(loser)) {
            throw new IllegalArgumentException("`" + loser + "`님은 등록되지 않은 인원입니다.");
        }

        repository.updateAmount(winner, amount);
        repository.updateAmount(loser, -amount);
    }

    public int transfer(String name, int amount) throws SQLException {
        if (!repository.existsByName(name)) {
            throw new IllegalArgumentException("`" + name + "`님은 등록되지 않은 인원입니다.");
        }
        repository.updateAmount(name, amount);
        return repository.getAmountByName(name);
    }

    public void complete(String name) throws SQLException {
        if (!repository.existsByName(name)) {
            throw new IllegalArgumentException("`" + name + "`님은 등록되지 않은 인원입니다.");
        }
        repository.setAmountZero(name);
    }

    public void resetAll() throws SQLException {
        repository.resetAllAmounts();
    }
}