package com.icthh.xm.ms.balance.service;

import static java.math.BigDecimal.ZERO;

import com.icthh.xm.commons.exceptions.EntityNotFoundException;
import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.repository.PermittedRepository;
import com.icthh.xm.ms.balance.domain.Balance;
import com.icthh.xm.ms.balance.domain.Pocket;
import com.icthh.xm.ms.balance.repository.BalanceRepository;
import com.icthh.xm.ms.balance.repository.PocketRepository;
import com.icthh.xm.ms.balance.service.dto.BalanceDTO;
import com.icthh.xm.ms.balance.service.mapper.BalanceMapper;
import com.icthh.xm.ms.balance.web.rest.requests.ReloadBalanceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;


/**
 * Service Implementation for managing Balance.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final PermittedRepository permittedRepository;
    private final PocketRepository pocketRepository;
    private final BalanceMapper balanceMapper;

    /**
     * Save a balance.
     *
     * @param balanceDTO the entity to save
     * @return the persisted entity
     */
    public BalanceDTO save(BalanceDTO balanceDTO) {
        Balance balance = balanceMapper.toEntity(balanceDTO);
        balance = balanceRepository.save(balance);
        return balanceMapper.toDto(balance);
    }

    /**
     * Get all the balances.
     *
     * @param pageable the pagination information
     * @return the list of entities
     */
    @FindWithPermission("BALANCE.GET_LIST")
    @Transactional(readOnly = true)
    public Page<BalanceDTO> findAll(Pageable pageable, String privilegeKey) {
        Page<Balance> page = permittedRepository.findAll(pageable, Balance.class, privilegeKey);
        List<BalanceDTO> dtos = page.map(balanceMapper::toDto).getContent();
        Map<Long, BigDecimal> balancesAmount = balanceRepository.getBalancesAmountMap(page.getContent());
        dtos.forEach(it -> it.setAmount(balancesAmount.getOrDefault(it.getId(), ZERO)));
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    /**
     * Get one balance by id.
     *
     * @param id the id of the entity
     * @return the entity
     */
    @Transactional(readOnly = true)
    public BalanceDTO findOne(Long id) {
        Balance balance = balanceRepository.findOne(id);
        BalanceDTO balanceDTO = balanceMapper.toDto(balance);
        if (balanceDTO != null) {
            balanceDTO.setAmount(balanceRepository.getBalanceAmount(balance).orElse(ZERO));
        }
        return balanceDTO;
    }

    /**
     * Delete the balance by id.
     *
     * @param id the id of the entity
     */
    public void delete(Long id) {
        balanceRepository.delete(id);
    }

    @Transactional
    public void reload(ReloadBalanceRequest reloadRequest) {
        log.info("Start reload balance with request {}", reloadRequest);

        Balance balance = balanceRepository.findOneByIdForUpdate(reloadRequest.getBalanceId())
            .orElseThrow(() -> new EntityNotFoundException("Balance with id " + reloadRequest.getBalanceId() + "not found"));

        log.debug("Found balance {}", balance);

        Pocket pocket = findPocketForReload(reloadRequest, balance)
            .map(Pocket::getId)
            .flatMap(pocketRepository::findOneByIdForUpdate)
            .map(existingPocket -> existingPocket.addAmount(reloadRequest.getAmount()))
            .orElse(new Pocket()
                .balance(balance)
                .amount(reloadRequest.getAmount())
                .endDateTime(reloadRequest.getEndDateTime())
                .startDateTime(reloadRequest.getStartDateTime())
                .label(reloadRequest.getLabel())
            );

        Pocket savedPocket = pocketRepository.save(pocket);
        log.info("Pocket affected by reload {}", savedPocket);
    }

    private Optional<Pocket> findPocketForReload(ReloadBalanceRequest reloadRequest, Balance balance) {
        Optional<Pocket> pocket = pocketRepository.findByLabelAndStartDateTimeAndEndDateTimeAndBalance(reloadRequest.getLabel(),
            reloadRequest.getStartDateTime(), reloadRequest.getEndDateTime(), balance);

        if (pocket.isPresent()) {
            log.info("Found pocket {} for reload", pocket);
        } else {
            log.info("Pocket for reload not found. New pocket will be created");
        }
        return pocket;
    }
}
