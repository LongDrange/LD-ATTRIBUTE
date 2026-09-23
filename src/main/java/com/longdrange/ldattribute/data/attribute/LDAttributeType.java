package com.longdrange.ldattribute.data.attribute;

/**
 * 屬性類型
 * 定義屬性在什麼時機被觸發
 */
public enum LDAttributeType {

    /**
     * 攻擊時觸發
     * 例如：攻擊力、暴擊、吸血、點燃、閃電
     */
    ATTACK,

    /**
     * 防禦時觸發（被攻擊方）
     * 例如：防禦力、閃避、格擋、反射
     */
    DEFENSE,

    /**
     * 屬性更新時觸發
     * 例如：生命上限、移動速度、生命回復
     */
    UPDATE,

    /**
     * 其他時機
     * 例如：經驗加成、掉落加成、事件訊息
     */
    OTHER;
}